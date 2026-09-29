import { useEffect, type ReactElement, type ReactNode } from 'react';

/**
 * Una hoja que sube desde abajo en movil y un modal centrado en escritorio. Lo decide CSS,
 * igual que el resto del armazon.
 *
 * Escape y el velo cierran. El tirador se pinta tambien en el modal, donde no sirve para
 * arrastrar nada: es lo que hace el diseno.
 */
export function Sheet({
  title,
  subtitle,
  onClose,
  children,
}: {
  title: string;
  subtitle?: string;
  onClose: () => void;
  children: ReactNode;
}): ReactElement {
  useEffect(() => {
    const onKey = (event: KeyboardEvent): void => {
      if (event.key === 'Escape') onClose();
    };
    globalThis.addEventListener('keydown', onKey);
    return () => {
      globalThis.removeEventListener('keydown', onKey);
    };
  }, [onClose]);
  return (
    <div className="g-sheet">
      <button aria-label="Cerrar" className="g-sheet__scrim" onClick={onClose} type="button" />
      <div aria-label={title} aria-modal="true" className="g-sheet__panel" role="dialog">
        <div className="g-sheet__handle" />
        <h2 className="g-sheet__title">{title}</h2>
        {subtitle === undefined ? null : <p className="g-sheet__subtitle">{subtitle}</p>}
        {children}
      </div>
    </div>
  );
}
