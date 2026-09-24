//! Handlers que publican la documentacion OpenAPI.

use std::sync::Arc;

use salvo::prelude::{Depot, FlowCtrl, Handler, Request, Response, StatusCode, Text};

/// Sirve el documento OpenAPI en `/openapi.json`.
///
/// El JSON se genera una sola vez al construir el router y se reparte por
/// referencia: el documento no cambia en caliente.
#[derive(Debug, Clone)]
pub struct OpenApiHandler {
    document: Arc<String>,
}

impl OpenApiHandler {
    /// Crea el handler a partir del documento ya serializado.
    #[must_use]
    pub fn new(document: String) -> Self {
        Self {
            document: Arc::new(document),
        }
    }
}

#[salvo::async_trait]
impl Handler for OpenApiHandler {
    async fn handle(
        &self,
        _req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        res.render_with_status(
            StatusCode::OK,
            Text::Json(self.document.as_str().to_owned()),
        );
    }
}

/// Sirve una pagina minima de Swagger UI en `/swagger-ui`.
///
/// La pagina carga los assets desde un CDN publico. Es una comodidad de
/// desarrollo: en un entorno sin salida a internet el endpoint responde, pero
/// la interfaz no se pinta. El contrato de verdad es `/openapi.json`.
#[derive(Debug, Clone, Copy, Default)]
pub struct SwaggerUiHandler;

impl SwaggerUiHandler {
    /// Crea el handler.
    #[must_use]
    pub const fn new() -> Self {
        Self
    }
}

const SWAGGER_UI_PAGE: &str = r##"<!doctype html>
<html lang="es">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Gastonomo API</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui.css" />
  </head>
  <body>
    <div id="swagger-ui"></div>
    <script src="https://cdn.jsdelivr.net/npm/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
    <script>
      window.SwaggerUIBundle({ url: "/openapi.json", dom_id: "#swagger-ui" });
    </script>
  </body>
</html>
"##;

#[salvo::async_trait]
impl Handler for SwaggerUiHandler {
    async fn handle(
        &self,
        _req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        res.render_with_status(StatusCode::OK, Text::Html(SWAGGER_UI_PAGE));
    }
}
