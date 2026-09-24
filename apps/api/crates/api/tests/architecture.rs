//! Test de arquitectura: el nucleo no puede arrastrar infraestructura.
//!
//! Lee `Cargo.lock` y calcula el cierre transitivo de dependencias de cada
//! crate del nucleo. Si alguien anade Salvo, SQLx, Tokio o reqwest a `domain` o
//! a `application`, este test falla y el commit no pasa.

#![allow(
    clippy::unwrap_used,
    clippy::expect_used,
    clippy::indexing_slicing,
    reason = "tests fail loudly on purpose; a panic here is the assertion"
)]
#![allow(
    unused_crate_dependencies,
    reason = "the lint runs per target and this test only reads Cargo.lock"
)]

use std::collections::{BTreeMap, BTreeSet, VecDeque};
use std::path::PathBuf;

/// Crates que el nucleo no puede alcanzar, ni directa ni transitivamente.
const FORBIDDEN: &[&str] = &[
    "salvo",
    "salvo_core",
    "salvo-oapi",
    "sqlx",
    "sqlx-core",
    "sqlx-postgres",
    "tokio",
    "reqwest",
    "hyper",
    "figment",
    "utoipa",
    "serde",
    "serde_json",
];

/// Crates del nucleo hexagonal.
const CORE_CRATES: &[&str] = &["gastonomo-domain", "gastonomo-application"];

fn lock_path() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR"))
        .join("..")
        .join("..")
        .join("Cargo.lock")
}

/// Mapa `paquete -> dependencias directas` tal y como lo declara `Cargo.lock`.
///
/// El fichero incluye tambien las dev-dependencies, asi que el test cubre el
/// nucleo en produccion y en tests.
fn dependency_graph(lock: &str) -> BTreeMap<String, BTreeSet<String>> {
    let mut graph = BTreeMap::new();
    let mut current: Option<String> = None;
    let mut in_dependencies = false;

    for line in lock.lines() {
        let trimmed = line.trim();
        if trimmed == "[[package]]" {
            current = None;
            in_dependencies = false;
        } else if let Some(name) = trimmed.strip_prefix("name = ") {
            current = Some(unquote(name));
            graph
                .entry(unquote(name))
                .or_insert_with(BTreeSet::<String>::new);
        } else if trimmed == "dependencies = [" {
            in_dependencies = true;
        } else if in_dependencies && trimmed == "]" {
            in_dependencies = false;
        } else if in_dependencies {
            push_dependency(&mut graph, current.as_deref(), trimmed);
        }
    }
    graph
}

fn push_dependency(graph: &mut BTreeMap<String, BTreeSet<String>>, owner: Option<&str>, raw: &str) {
    let Some(owner) = owner else { return };
    let entry = unquote(raw.trim_end_matches(','));
    let Some(name) = entry.split_whitespace().next() else {
        return;
    };
    graph
        .entry(owner.to_owned())
        .or_default()
        .insert(name.to_owned());
}

fn unquote(raw: &str) -> String {
    raw.trim().trim_matches('"').to_owned()
}

fn transitive_closure(graph: &BTreeMap<String, BTreeSet<String>>, root: &str) -> BTreeSet<String> {
    let mut seen = BTreeSet::new();
    let mut queue = VecDeque::new();
    queue.push_back(root.to_owned());
    while let Some(package) = queue.pop_front() {
        let Some(dependencies) = graph.get(&package) else {
            continue;
        };
        for dependency in dependencies {
            if seen.insert(dependency.clone()) {
                queue.push_back(dependency.clone());
            }
        }
    }
    seen
}

fn graph() -> BTreeMap<String, BTreeSet<String>> {
    let lock = std::fs::read_to_string(lock_path())
        .expect("Cargo.lock should exist at the workspace root");
    dependency_graph(&lock)
}

#[test]
fn the_lock_file_describes_every_workspace_crate() {
    let graph = graph();
    for crate_name in [
        "gastonomo-domain",
        "gastonomo-application",
        "gastonomo-infrastructure",
        "gastonomo-http",
        "gastonomo-api",
    ] {
        assert!(
            graph.contains_key(crate_name),
            "{crate_name} is missing from Cargo.lock; the parser is out of date"
        );
    }
}

#[test]
fn the_core_never_depends_on_infrastructure() {
    let graph = graph();
    for core in CORE_CRATES {
        let reachable = transitive_closure(&graph, core);
        for forbidden in FORBIDDEN {
            assert!(
                !reachable.contains(*forbidden),
                "{core} reaches `{forbidden}`. The hexagon leaks: move that code to an adapter.\n\
                 Reachable crates: {reachable:?}"
            );
        }
    }
}

#[test]
fn the_domain_only_depends_on_thiserror() {
    let graph = graph();
    let direct = graph
        .get("gastonomo-domain")
        .expect("gastonomo-domain should be in Cargo.lock");
    assert_eq!(
        direct.iter().map(String::as_str).collect::<Vec<_>>(),
        vec!["thiserror"],
        "the domain crate must stay dependency free beyond thiserror"
    );
}

#[test]
fn the_application_layer_only_depends_on_the_domain_and_trait_helpers() {
    let graph = graph();
    let direct = graph
        .get("gastonomo-application")
        .expect("gastonomo-application should be in Cargo.lock");
    assert_eq!(
        direct.iter().map(String::as_str).collect::<Vec<_>>(),
        vec!["async-trait", "gastonomo-domain", "thiserror"],
        "the application layer must only declare ports, never adapters"
    );
}

#[test]
fn the_adapters_are_allowed_to_use_infrastructure() {
    let graph = graph();
    let http = transitive_closure(&graph, "gastonomo-http");
    assert!(http.contains("salvo"), "the http adapter should use salvo");
    let infrastructure = transitive_closure(&graph, "gastonomo-infrastructure");
    assert!(
        infrastructure.contains("sqlx"),
        "the persistence adapter should use sqlx"
    );
}
