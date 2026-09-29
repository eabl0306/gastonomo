import type { ReactElement } from 'react';
import { NavLink, Outlet } from 'react-router';

import { initialsOf, type User } from '../../domain/account/user';
import { Icon } from '../auth/auth-chrome';
import { RangeSheet } from '../period/range-sheet';
import { useSession } from '../session/use-session';

import { NAV_ITEMS, type NavItem } from './nav-items';

/**
 * El armazon de la aplicacion.
 *
 * El corte entre la barra lateral de escritorio y la navegacion inferior de movil **lo
 * decide CSS y no JavaScript**: las dos se pintan siempre y el CSS esconde la que sobra. Con
 * un `matchMedia` en estado habria un primer fotograma con el armazon equivocado, y encima
 * el servidor de desarrollo lo notaria en cada recarga.
 *
 * El sheet de rango se monta aqui y no dentro de una pantalla porque el borrador es el mismo
 * para Inicio y para Movimientos: uno por pantalla serian dos borradores distintos.
 */
export function AppShell(): ReactElement {
  const session = useSession();
  return (
    <div className="g-shell">
      <Sidebar user={session?.user ?? null} />
      <div className="g-shell__column">
        <main className="g-shell__main">
          <Outlet />
        </main>
        <BottomNav />
      </div>
      <RangeSheet />
    </div>
  );
}

function Sidebar({ user }: { user: User | null }): ReactElement {
  return (
    <aside className="g-side">
      <div className="g-side__brand">
        <div className="g-side__brand-tile">g</div>
        <span className="g-side__brand-word">gastonomo</span>
      </div>
      <nav aria-label="Secciones" className="g-side__nav">
        {NAV_ITEMS.map((item) => (
          <SidebarItem item={item} key={item.to} />
        ))}
      </nav>
      <div className="g-side__spacer" />
      {user === null ? null : <SidebarUser user={user} />}
    </aside>
  );
}

function SidebarItem({ item }: { item: NavItem }): ReactElement {
  return (
    <NavLink className="g-side__item" end={item.to === '/'} to={item.to}>
      {({ isActive }) => (
        <>
          <Icon filled={isActive} name={item.icon} />
          {item.label}
        </>
      )}
    </NavLink>
  );
}

function SidebarUser({ user }: { user: User }): ReactElement {
  return (
    <div className="g-side__user">
      <div className="g-side__avatar">{initialsOf(user)}</div>
      <div className="g-side__identity">
        <p className="g-side__name">{user.name}</p>
        <p className="g-side__email">{user.email}</p>
      </div>
    </div>
  );
}

function BottomNav(): ReactElement {
  return (
    <nav aria-label="Secciones" className="g-tabs">
      {NAV_ITEMS.map((item) => (
        <NavLink className="g-tabs__item" end={item.to === '/'} key={item.to} to={item.to}>
          {({ isActive }) => (
            <>
              <span className="g-tabs__pill">
                <Icon filled={isActive} name={item.icon} />
              </span>
              {item.label}
            </>
          )}
        </NavLink>
      ))}
    </nav>
  );
}
