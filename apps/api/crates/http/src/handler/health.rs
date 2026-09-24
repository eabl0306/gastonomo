//! Handlers de `/health/live` y `/health/ready`.

use std::sync::Arc;

use gastonomo_application::{CheckLiveness, CheckReadiness};
use gastonomo_domain::HealthReport;
use salvo::prelude::{Depot, FlowCtrl, Handler, Json, Request, Response, StatusCode};

use crate::dto::HealthResponseDto;

/// Responde a `/health/live`.
///
/// No consulta dependencias: siempre devuelve 200 mientras el proceso atienda.
#[derive(Debug, Clone)]
pub struct LivenessHandler {
    use_case: CheckLiveness,
}

impl LivenessHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: CheckLiveness) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for LivenessHandler {
    async fn handle(
        &self,
        _req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        write_report(res, &self.use_case.execute(), StatusCode::OK);
    }
}

/// Responde a `/health/ready`.
///
/// Devuelve 200 cuando todos los componentes responden y 503 en cuanto uno
/// falla, tal y como exige el contrato compartido con la web y Android.
#[derive(Debug, Clone)]
pub struct ReadinessHandler {
    use_case: Arc<CheckReadiness>,
}

impl ReadinessHandler {
    /// Crea el handler sobre el caso de uso.
    #[must_use]
    pub const fn new(use_case: Arc<CheckReadiness>) -> Self {
        Self { use_case }
    }
}

#[salvo::async_trait]
impl Handler for ReadinessHandler {
    async fn handle(
        &self,
        _req: &mut Request,
        _depot: &mut Depot,
        res: &mut Response,
        _ctrl: &mut FlowCtrl,
    ) {
        let report = self.use_case.execute().await;
        let status = if report.is_healthy() {
            StatusCode::OK
        } else {
            StatusCode::SERVICE_UNAVAILABLE
        };
        if !report.is_healthy() {
            tracing::warn!(
                failing = ?report.failing_components(),
                "readiness check reported a degraded service"
            );
        }
        write_report(res, &report, status);
    }
}

fn write_report(res: &mut Response, report: &HealthReport, status: StatusCode) {
    res.render_with_status(status, Json(HealthResponseDto::from(report)));
}
