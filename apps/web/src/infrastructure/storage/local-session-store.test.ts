import { beforeEach, describe, expect, it } from 'vitest';

import type { Session } from '../../domain/account/session';

import { createLocalSessionStore } from './local-session-store';

const SESSION: Session = {
  accessToken: 'a1',
  expiresInSeconds: 900,
  refreshToken: 'r1',
  user: { id: 'u1', name: 'Camilo', email: 'camilo@correo.com' },
};

describe('local session store', () => {
  beforeEach(() => {
    globalThis.localStorage.clear();
  });

  it('sin nada guardado no hay sesion', () => {
    expect(createLocalSessionStore().read()).toBeNull();
  });

  it('guarda los tokens y el usuario, y los recupera tras una recarga', () => {
    createLocalSessionStore().save(SESSION);
    const afterReload = createLocalSessionStore().read();
    expect(afterReload).toEqual({ accessToken: 'a1', refreshToken: 'r1', user: SESSION.user });
  });

  it('rotar cambia los tokens y deja el usuario', () => {
    const store = createLocalSessionStore();
    store.save(SESSION);
    store.rotate({ accessToken: 'a2', refreshToken: 'r2', expiresInSeconds: 900 });
    expect(store.read()).toEqual({ accessToken: 'a2', refreshToken: 'r2', user: SESSION.user });
  });

  it('rotar sin sesion no inventa una', () => {
    const store = createLocalSessionStore();
    store.rotate({ accessToken: 'a2', refreshToken: 'r2', expiresInSeconds: 900 });
    expect(store.read()).toBeNull();
  });

  it('salir borra lo guardado', () => {
    const store = createLocalSessionStore();
    store.save(SESSION);
    store.clear();
    expect(store.read()).toBeNull();
  });

  /** Una sesion a medias manda peticiones sin token y echa al usuario de formas raras. */
  it('lo guardado que no cuadra se borra en vez de usarse a medias', () => {
    globalThis.localStorage.setItem('gastonomo.session', '{"accessToken":"a1"}');
    expect(createLocalSessionStore().read()).toBeNull();
    expect(globalThis.localStorage.getItem('gastonomo.session')).toBeNull();
  });

  it('lo guardado que ni siquiera es JSON tampoco tumba nada', () => {
    globalThis.localStorage.setItem('gastonomo.session', 'no soy json');
    expect(createLocalSessionStore().read()).toBeNull();
  });

  it('avisa a quien escucha cuando la sesion cambia', () => {
    const store = createLocalSessionStore();
    let changes = 0;
    const stop = store.subscribe(() => {
      changes += 1;
    });
    store.save(SESSION);
    store.clear();
    stop();
    store.save(SESSION);
    expect(changes).toBe(2);
  });
});
