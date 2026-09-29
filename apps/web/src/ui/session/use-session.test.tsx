import { act, render, screen } from '@testing-library/react';
import type { ReactElement } from 'react';
import { describe, expect, it } from 'vitest';

import type { AuthGateway } from '../../application/ports/auth-gateway';
import type { SessionStore, StoredSession } from '../../application/ports/session-store';
import type { Session } from '../../domain/account/session';

import { SessionProvider } from './session-context';
import { useSession } from './use-session';

const USER = { id: 'u1', name: 'Camilo', email: 'camilo@correo.com' };

function storeOf(initial: StoredSession | null): SessionStore {
  let current = initial;
  const listeners = new Set<() => void>();
  const announce = (): void => {
    for (const listener of listeners) listener();
  };
  return {
    read: () => current,
    save: (session: Session) => {
      current = {
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
        user: session.user,
      };
      announce();
    },
    rotate: () => undefined,
    clear: () => {
      current = null;
      announce();
    },
    subscribe: (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };
}

const AUTH = {} as AuthGateway;

function Probe(): ReactElement {
  const session = useSession();
  return <p>{session === null ? 'fuera' : session.user.name}</p>;
}

function renderWith(session: SessionStore): void {
  render(
    <SessionProvider value={{ session, auth: AUTH }}>
      <Probe />
    </SessionProvider>,
  );
}

describe('useSession', () => {
  it('sin sesion guardada, fuera', () => {
    renderWith(storeOf(null));
    expect(screen.getByText('fuera')).toBeInTheDocument();
  });

  it('con sesion guardada, dentro desde el primer pintado', () => {
    renderWith(storeOf({ accessToken: 'a1', refreshToken: 'r1', user: USER }));
    expect(screen.getByText('Camilo')).toBeInTheDocument();
  });

  /** La sesion la puede cambiar el cliente al refrescar, o incluso otra pestana. */
  it('se vuelve a pintar cuando la sesion cambia desde fuera', () => {
    const store = storeOf(null);
    renderWith(store);
    act(() => {
      store.save({
        accessToken: 'a1',
        expiresInSeconds: 900,
        refreshToken: 'r1',
        user: USER,
      });
    });
    expect(screen.getByText('Camilo')).toBeInTheDocument();
    act(() => {
      store.clear();
    });
    expect(screen.getByText('fuera')).toBeInTheDocument();
  });

  it('sin proveedor avisa en vez de fallar mas tarde', () => {
    expect(() => render(<Probe />)).toThrow(/proveedor de sesion/u);
  });
});
