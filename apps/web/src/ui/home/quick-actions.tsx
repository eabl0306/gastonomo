import type { ReactElement } from 'react';

import type { MovementKind } from '../../domain/ledger/category';
import { Icon } from '../auth/auth-chrome';

export interface QuickActionsProps {
  readonly onNewMovement: (kind: MovementKind) => void;
  readonly onNewLoan: () => void;
  readonly onOpenLoans: () => void;
}

/**
 * Las cuatro acciones rapidas.
 *
 * Tres abren un formulario y la de Abono lleva a Prestamos, que es donde se elige a quien se
 * le abona: un abono no existe sin prestamo.
 */
export function QuickActions(props: QuickActionsProps): ReactElement {
  return (
    <div className="g-quick">
      <Action
        icon="south_west"
        label="Ingreso"
        onClick={() => {
          props.onNewMovement('in');
        }}
        tone="income"
      />
      <Action
        icon="north_east"
        label="Gasto"
        onClick={() => {
          props.onNewMovement('out');
        }}
        tone="expense"
      />
      <Action icon="handshake" label="Préstamo" onClick={props.onNewLoan} tone="loan" />
      <Action icon="add_card" label="Abono" onClick={props.onOpenLoans} tone="payment" />
    </div>
  );
}

function Action({
  label,
  icon,
  tone,
  onClick,
}: {
  label: string;
  icon: string;
  tone: 'income' | 'expense' | 'loan' | 'payment';
  onClick: () => void;
}): ReactElement {
  return (
    <button className="g-quick__item" onClick={onClick} type="button">
      <span className="g-quick__tile" data-tone={tone}>
        <Icon name={icon} />
      </span>
      {label}
    </button>
  );
}
