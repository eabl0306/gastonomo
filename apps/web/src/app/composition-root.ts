import type { AuthGateway } from '../application/ports/auth-gateway';
import type { CategoryGateway } from '../application/ports/category-gateway';
import type { Clock } from '../application/ports/clock';
import type { CurrencyGateway } from '../application/ports/currency-gateway';
import type { ExportWriter, FileDownloader } from '../application/ports/file-download';
import type { HealthGateway } from '../application/ports/health-gateway';
import type { LoanGateway } from '../application/ports/loan-gateway';
import type { MovementGateway } from '../application/ports/movement-gateway';
import type { PasswordResetGateway } from '../application/ports/password-reset-gateway';
import type { SessionStore } from '../application/ports/session-store';
import type { SettingsGateway } from '../application/ports/settings-gateway';
import { browserDownloader } from '../infrastructure/export/browser-downloader';
import { exportWriter } from '../infrastructure/export/serialize';
import { createApiClient, type ApiClient } from '../infrastructure/http/api-client';
import type { RotatedTokens } from '../infrastructure/http/api-tokens';
import { createHttpAuthGateway } from '../infrastructure/http/http-auth-gateway';
import { createHttpCategoryGateway } from '../infrastructure/http/http-category-gateway';
import { createHttpCurrencyGateway } from '../infrastructure/http/http-currency-gateway';
import { createHttpHealthGateway } from '../infrastructure/http/http-health-gateway';
import { createHttpLoanGateway } from '../infrastructure/http/http-loan-gateway';
import { createHttpMovementGateway } from '../infrastructure/http/http-movement-gateway';
import { createHttpPasswordResetGateway } from '../infrastructure/http/http-password-reset-gateway';
import { createHttpSettingsGateway } from '../infrastructure/http/http-settings-gateway';
import { createLocalSessionStore } from '../infrastructure/storage/local-session-store';
import { systemClock } from '../infrastructure/time/system-clock';

/** Lo unico que la web necesita saber del entorno. */
export interface WebEnv {
  readonly VITE_API_URL?: string;
}

/**
 * Vacio significa **el mismo origen**, que es como se sirve desplegado: Caddy pone la API
 * bajo el dominio de la web, asi que una ruta relativa acierta. En desarrollo lo consigue el
 * proxy de Vite. `VITE_API_URL` solo hace falta para apuntar a una API de otro sitio.
 */
const SAME_ORIGIN = '';
const DEFAULT_TIMEOUT_MS = 5000;

export function resolveApiUrl(env: WebEnv): string {
  return env.VITE_API_URL?.trim() ?? SAME_ORIGIN;
}

/**
 * Raiz de composicion: el unico sitio del arbol donde la interfaz y los adaptadores se
 * conocen. Cambiar de adaptador se hace aqui y en ningun otro lado.
 */
export function createHealthGateway(env: WebEnv): HealthGateway {
  return createHttpHealthGateway({
    baseUrl: resolveApiUrl(env),
    timeoutMs: DEFAULT_TIMEOUT_MS,
  });
}

/**
 * El restablecimiento tiene su propio plazo: cambiar la contrasena hashea con Argon2 en el
 * servidor, que tarda mas que responder una consulta.
 */
const RESET_TIMEOUT_MS = 10_000;

function createPasswordResetGateway(env: WebEnv): PasswordResetGateway {
  return createHttpPasswordResetGateway({
    baseUrl: resolveApiUrl(env),
    timeoutMs: RESET_TIMEOUT_MS,
  });
}

/** Todo lo que la interfaz recibe ya montado. */
export interface Container {
  readonly session: SessionStore;
  readonly auth: AuthGateway;
  readonly health: HealthGateway;
  readonly passwordResets: PasswordResetGateway;
  readonly movements: MovementGateway;
  readonly loans: LoanGateway;
  readonly settings: SettingsGateway;
  readonly currencies: CurrencyGateway;
  readonly categories: CategoryGateway;
  readonly exports: ExportWriter;
  readonly downloader: FileDownloader;
  readonly clock: Clock;
}

/**
 * El cliente y el gateway de autenticacion se necesitan mutuamente: el cliente refresca
 * llamando a la API, y quien sabe llamar a la API es el gateway, que a su vez usa el
 * cliente. Se rompe con una referencia perezosa: el gateway ya existe cuando alguien
 * refresca de verdad, porque eso solo pasa despues del primer 401.
 */
function createAuthenticatedClient(
  env: WebEnv,
  session: SessionStore,
): {
  client: ApiClient;
  auth: AuthGateway;
} {
  let auth: AuthGateway | null = null;
  const client = createApiClient({
    baseUrl: resolveApiUrl(env),
    session,
    refresh: async (refreshToken): Promise<RotatedTokens | null> => {
      const outcome = await auth?.refresh(refreshToken);
      if (outcome?.ok !== true) return null;
      return {
        accessToken: outcome.value.accessToken,
        refreshToken: outcome.value.refreshToken,
        expiresInSeconds: outcome.value.expiresInSeconds,
      };
    },
  });
  auth = createHttpAuthGateway(client);
  return { client, auth };
}

export function createContainer(env: WebEnv): Container {
  const session = createLocalSessionStore();
  const { client, auth } = createAuthenticatedClient(env, session);
  return {
    session,
    auth,
    health: createHealthGateway(env),
    passwordResets: createPasswordResetGateway(env),
    movements: createHttpMovementGateway(client),
    loans: createHttpLoanGateway(client),
    settings: createHttpSettingsGateway(client),
    currencies: createHttpCurrencyGateway(client),
    categories: createHttpCategoryGateway(client),
    exports: exportWriter,
    downloader: browserDownloader,
    clock: systemClock,
  };
}
