# Comprobaciones de calidad compartidas

Tres scripts de Node sin dependencias externas. Los usan los tres apps del monorepo,
porque las reglas que vigilan valen igual en TypeScript, en Rust y en Kotlin, y ningun
linter de un solo lenguaje las cubre todas.

Son un paquete del workspace (`@gastonomo/checks`), pero se invocan por ruta con `node`,
que es lo que hacen los hooks y la CI.

| Script                    | Que rompe                                                        |
| ------------------------- | ---------------------------------------------------------------- |
| `file-size.mjs`           | Ficheros de codigo de mas de 300 lineas, o de test de mas de 400 |
| `no-focused-tests.mjs`    | `.only(`, `#[ignore]` y `@Ignore` sin motivo                     |
| `todo-without-ticket.mjs` | Marcadores de trabajo pendiente sin referencia a ticket          |

## Uso

```bash
node tools/checks/file-size.mjs             # todo el repo
node tools/checks/file-size.mjs --staged    # solo lo que esta en el index de git
node tools/checks/file-size.mjs --root /tmp/x   # otra raiz (lo usan sus tests)
```

Todos imprimen un informe con ruta y linea, y salen con codigo 1 si hay violaciones.

## Detalles que conviene saber

- Se recorren `apps/api`, `apps/web`, `apps/android`, `packages` y `tools`. Se saltan
  `node_modules`, `target`, `build`, `dist`, `coverage`, `.gradle` y compania.
- Quedan fuera lo generado, las migraciones SQL y los recursos de Android.
- El contador de lineas cuenta lineas fisicas. ESLint, que ademas salta lineas en blanco
  y comentarios, es mas fino dentro de JavaScript y TypeScript: este script es el limite
  de arriba, comun a los tres lenguajes.
- Los marcadores se comparan distinguiendo mayusculas, porque en castellano "todo" es una
  palabra corriente y no puede disparar la comprobacion. El formato valido es
  `MARCADOR(ABC-123): ...`.
- Los scripts construyen esas palabras por concatenacion para no delatarse a si mismos.

## Tests

```bash
pnpm run check:tools-test
```

Cada script se prueba de verdad: se monta un directorio temporal con ficheros que
violan la regla, se ejecuta el script y se comprueba el codigo de salida.
