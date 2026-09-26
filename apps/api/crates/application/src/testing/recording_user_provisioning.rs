//! Siembra de cuenta nueva de test: apunta a quien se sembro.

use std::sync::Mutex;

use async_trait::async_trait;
use chrono::{DateTime, Utc};
use gastonomo_domain::UserId;

use crate::error::AppError;
use crate::port::UserProvisioning;

/// Doble del puerto [`UserProvisioning`].
#[derive(Debug, Default)]
pub struct RecordingUserProvisioning {
    provisioned: Mutex<Vec<UserId>>,
}

impl RecordingUserProvisioning {
    /// Crea el doble.
    #[must_use]
    pub const fn new() -> Self {
        Self {
            provisioned: Mutex::new(Vec::new()),
        }
    }

    /// Cuentas sembradas, en orden.
    #[must_use]
    pub fn provisioned(&self) -> Vec<UserId> {
        self.provisioned
            .lock()
            .map_or_else(|poisoned| poisoned.into_inner().clone(), |ids| ids.clone())
    }
}

#[async_trait]
impl UserProvisioning for RecordingUserProvisioning {
    async fn provision_new_user(
        &self,
        user_id: UserId,
        _now: DateTime<Utc>,
    ) -> Result<(), AppError> {
        if let Ok(mut provisioned) = self.provisioned.lock() {
            provisioned.push(user_id);
        }
        Ok(())
    }
}
