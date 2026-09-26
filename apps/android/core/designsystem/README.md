# Sistema de diseno

Los tokens y los componentes del rediseno, sacados del handoff y del prototipo HTML. Cuando
el README del handoff y el HTML discrepaban, gana el HTML.

## Como se usa

```kotlin
GastonomoTheme(language = settings.language, displayCurrency = settings.displayCurrency) {
    GastonomoScreen {
        GastonomoText("Mercado", style = GastonomoTheme.typography.body)
        PrimaryButton(text = stringResource(R.string.save), enabled = valid, onClick = ::save)
    }
}
```

Todos los tokens salen del objeto `GastonomoTheme`: `colors`, `typography`, `radii`,
`dimens`, `shadows` y `formats`. No hay colores ni tamanos sueltos en ninguna pantalla.

## Las fuentes

`res/font` lleva tres ficheros:

| Fichero                              | Que es                                    | Peso   |
|--------------------------------------|-------------------------------------------|--------|
| `plus_jakarta_sans.ttf`              | Variable, eje `wght`. Cubre los 5 pesos.  | 172 KB |
| `material_symbols_rounded.ttf`       | Material Symbols Rounded con `FILL 0`     | 19 KB  |
| `material_symbols_rounded_filled.ttf`| La misma con `FILL 1`, para el tab activo | 26 KB  |

Los iconos se pintan escribiendo su nombre como texto, y la ligadura de la fuente lo
convierte en glifo, igual que hace el prototipo. Se eligio esto y no
`material-icons-extended` porque esa biblioteca no tiene los glifos Rounded ni el eje FILL.

La fuente completa de Material Symbols Rounded pesa **15 MB**, asi que va recortada a los
65 iconos que usa el diseno. Para anadir uno:

1. Anadelo a `GastonomoIcons` (constante y lista `ALL`).
2. Vuelve a pedir las dos fuentes recortadas a Google Fonts, con la lista entera:

```bash
ICONS="add,add_card,arrow_back"    # la lista completa de GastonomoIcons.ALL, separada por comas
UA="Mozilla/5.0 (Linux; U; Android 4.0.3; en-us; Galaxy Nexus Build/IML74K) \
AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30"
for fill in 0 1; do
  css=$(curl -s -A "$UA" \
    "https://fonts.googleapis.com/css2?family=Material+Symbols+Rounded:opsz,wght,FILL,GRAD@24,400,$fill,0&icon_names=$ICONS")
  url=$(printf '%s' "$css" | grep -o 'https://fonts.gstatic.com/[^)]*' | head -1)
  curl -s -o "ms_fill$fill.ttf" "$url"
done
```

El `User-Agent` antiguo es lo que hace que Google sirva **TTF** en vez de WOFF2, que Compose
no sabe leer.

## Capturas

`ComponentGallery` y `OverlayGallery` son el catalogo entero en una pantalla. No son
pantallas del producto y sus textos van en duro a proposito. Los tests de
`gallery/` los renderizan a PNG en `build/outputs/roborazzi/`.
