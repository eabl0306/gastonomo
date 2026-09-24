package com.bahoque.gastonomo.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.library(alias: String): Provider<MinimalExternalModuleDependency> =
    libs.findLibrary(alias).orElseThrow { missingAlias("library", alias) }

internal fun Project.bundle(alias: String): Provider<ExternalModuleDependencyBundle> =
    libs.findBundle(alias).orElseThrow { missingAlias("bundle", alias) }

private fun missingAlias(kind: String, alias: String): IllegalStateException =
    IllegalStateException("Missing $kind '$alias' in gradle/libs.versions.toml")
