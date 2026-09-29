import type { MovementKind } from '../../domain/ledger/category';
import type { Movement } from '../../domain/ledger/movement';
import type { MonetaryAmount } from '../../domain/money/monetary-amount';

import type { Outcome } from './api-error';

/** El filtro de `GET /v1/movements`. Todo es opcional: sin nada, la API pagina desde hoy. */
export interface MovementQuery {
  /** Dia inclusivo, en `YYYY-MM-DD`. */
  readonly from?: string;
  readonly to?: string;
  readonly kind?: MovementKind;
  /** El `nextCursor` de la pagina anterior. */
  readonly cursor?: string;
  readonly limit?: number;
}

/**
 * Una pagina de movimientos.
 *
 * El cursor vive aqui y no en el dominio porque es de la lectura, no del ledger: un
 * movimiento no sabe en que pagina cayo.
 */
export interface MovementPage {
  readonly items: readonly Movement[];
  /** Null cuando ya no queda nada mas atras. */
  readonly nextCursor: string | null;
}

/** Lo que se manda a `POST /v1/movements`. */
export interface MovementDraft {
  readonly occurredOn: string;
  readonly kind: MovementKind;
  readonly categoryId: string;
  /** La nota del usuario. Vacia significa que el titulo es el nombre de la categoria. */
  readonly note: string;
  readonly money: MonetaryAmount;
}

export interface MovementGateway {
  list: (query: MovementQuery) => Promise<Outcome<MovementPage>>;
  /**
   * Da de alta un movimiento.
   *
   * [idempotencyKey] no es opcional a proposito: el historico no se puede borrar, asi que
   * un reintento sin clave deja el gasto dos veces y no hay pantalla para arreglarlo.
   */
  create: (draft: MovementDraft, idempotencyKey: string) => Promise<Outcome<Movement>>;
}
