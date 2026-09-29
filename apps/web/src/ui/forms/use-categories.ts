import { useEffect, useState } from 'react';

import type { CategoryGateway } from '../../application/ports/category-gateway';
import type { Category } from '../../domain/ledger/category';

/**
 * Las categorias que el formulario ofrece.
 *
 * Se piden **sin las archivadas**: una archivada ya no se ofrece al registrar, aunque sus
 * movimientos la sigan usando. Un fallo deja la lista vacia y el boton de guardar apagado, que
 * es mejor que guardar con una categoria inventada.
 */
export function useCategories(gateway: CategoryGateway): readonly Category[] {
  const [categories, setCategories] = useState<readonly Category[]>([]);
  useEffect(() => {
    let live = true;
    void gateway.list(false).then((outcome) => {
      if (live && outcome.ok) setCategories(outcome.value);
    });
    return () => {
      live = false;
    };
  }, [gateway]);
  return categories;
}
