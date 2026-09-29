import { useState, type ReactElement } from 'react';

import type { AuthGateway } from '../../application/ports/auth-gateway';
import type { CurrencyGateway } from '../../application/ports/currency-gateway';
import type { SettingsGateway } from '../../application/ports/settings-gateway';
import { signOut } from '../../application/use-cases/sign-out';
import { initialsOf, type User } from '../../domain/account/user';
import type { CurrencyCode } from '../../domain/money/currency';
import { useAccount } from '../account/account-context';
import { Icon } from '../auth/auth-chrome';
import { useFormats } from '../format/formats-context';
import { CurrencySheet } from '../forms/currency-sheet';
import { useSessionPorts } from '../session/session-context';
import { useSession } from '../session/use-session';
import { SettingsGroup, SettingsLink, SettingsStatus } from '../shell/settings-rows';

import { ChangePasswordSheet } from './change-password-sheet';

export interface SettingsPorts {
  readonly settings: SettingsGateway;
  readonly currencies: CurrencyGateway;
  readonly auth: AuthGateway;
}

export interface SettingsActions {
  readonly onOpenCategories: () => void;
  readonly onOpenExport: () => void;
  readonly onSignedOut: () => void;
}

/** Que sheet esta abierto. Solo puede haber uno. */
type OpenSheet = 'currency' | 'password' | null;

/**
 * Ajustes: el perfil, la moneda principal, el estado de las tasas, los atajos a categorias y
 * a exportar, y cerrar sesion.
 *
 * Cerrar sesion vive aqui y no en la barra lateral porque es donde lo pone el diseno, y porque
 * un boton de salir a un clic de distancia en toda la aplicacion se pulsa sin querer.
 */
export function SettingsScreen({
  ports,
  actions,
}: {
  ports: SettingsPorts;
  actions: SettingsActions;
}): ReactElement {
  const stored = useSession();
  const [sheet, setSheet] = useState<OpenSheet>(null);
  const close = (): void => {
    setSheet(null);
  };
  return (
    <div className="g-settings">
      <h1 className="g-screen__title">Ajustes</h1>
      {stored === null ? null : <ProfileCard user={stored.user} />}
      <div className="g-settings__groups">
        <General
          onOpenCurrency={() => {
            setSheet('currency');
          }}
        />
        <Data onOpenCategories={actions.onOpenCategories} onOpenExport={actions.onOpenExport} />
        <Account
          onChangePassword={() => {
            setSheet('password');
          }}
        />
      </div>
      <SignOutButton onSignedOut={actions.onSignedOut} />
      <p className="g-settings__version">Gastonomo 0.1.0 · Finanzas personales</p>
      <Sheets auth={ports.auth} onClose={close} settings={ports.settings} sheet={sheet} />
    </div>
  );
}

function Sheets({
  sheet,
  settings,
  auth,
  onClose,
}: {
  sheet: OpenSheet;
  settings: SettingsGateway;
  auth: AuthGateway;
  onClose: () => void;
}): ReactElement | null {
  const account = useAccount();
  if (sheet === 'password') {
    return <ChangePasswordSheet auth={auth} onChanged={onClose} onClose={onClose} />;
  }
  if (sheet !== 'currency') return null;
  return (
    <CurrencySheet
      catalog={account.catalog}
      onClose={onClose}
      onPick={(code) => {
        applyCurrency(settings, account.apply, code, account.settings.language);
        onClose();
      }}
      rates={account.rates}
      selected={account.settings.displayCurrency}
      subtitle="Los totales se mostrarán en esta moneda y será la moneda por defecto de tus registros nuevos. Los registros existentes no cambian."
      title="Moneda principal"
    />
  );
}

/**
 * Cambia la moneda principal.
 *
 * Se aplica en local antes de que conteste la API: la pantalla se repinta entera y esperar a
 * la respuesta deja las cifras en la moneda vieja durante medio segundo. Si falla, el proximo
 * arranque vuelve a la de verdad.
 */
function applyCurrency(
  gateway: SettingsGateway,
  apply: (settings: { displayCurrency: CurrencyCode; language: 'es' | 'en' }) => void,
  displayCurrency: CurrencyCode,
  language: 'es' | 'en',
): void {
  apply({ displayCurrency, language });
  void gateway.update({ displayCurrency });
}

function ProfileCard({ user }: { user: User }): ReactElement {
  return (
    <div className="g-profile">
      <div className="g-profile__avatar">{initialsOf(user)}</div>
      <div className="g-profile__identity">
        <p className="g-profile__name">{user.name}</p>
        <p className="g-profile__email">{user.email}</p>
      </div>
    </div>
  );
}

function General({ onOpenCurrency }: { onOpenCurrency: () => void }): ReactElement {
  const formats = useFormats();
  const { settings, rates, ratesUnavailable } = useAccount();
  return (
    <SettingsGroup
      note="Cada registro guarda su moneda original y su equivalencia. Cambiar la moneda solo cambia cómo ves las cifras y la moneda por defecto de los registros nuevos."
      title="General"
    >
      <SettingsLink
        icon="payments"
        label="Moneda"
        onClick={onOpenCurrency}
        value={settings.displayCurrency}
      />
      <SettingsStatus
        icon="currency_exchange"
        label="Tasas de cambio"
        tone={ratesUnavailable ? 'muted' : 'ok'}
        value={
          ratesUnavailable || rates.date === null ? 'Sin conexión' : formats.dates.short(rates.date)
        }
      />
    </SettingsGroup>
  );
}

function Data({
  onOpenCategories,
  onOpenExport,
}: {
  onOpenCategories: () => void;
  onOpenExport: () => void;
}): ReactElement {
  return (
    <SettingsGroup title="Datos">
      <SettingsLink icon="sell" label="Categorías" onClick={onOpenCategories} value="" />
      <SettingsLink
        icon="download"
        label="Exportar datos"
        onClick={onOpenExport}
        value="CSV · Excel"
      />
    </SettingsGroup>
  );
}

function Account({ onChangePassword }: { onChangePassword: () => void }): ReactElement {
  return (
    <SettingsGroup title="Cuenta">
      <SettingsLink icon="lock" label="Cambiar contraseña" onClick={onChangePassword} value="" />
    </SettingsGroup>
  );
}

function SignOutButton({ onSignedOut }: { onSignedOut: () => void }): ReactElement {
  const { auth, session } = useSessionPorts();
  const [leaving, setLeaving] = useState(false);
  return (
    <button
      className="g-signout"
      disabled={leaving}
      onClick={() => {
        setLeaving(true);
        void signOut(auth, session).then(onSignedOut);
      }}
      type="button"
    >
      <Icon name="logout" />
      {leaving ? 'Cerrando…' : 'Cerrar sesión'}
    </button>
  );
}
