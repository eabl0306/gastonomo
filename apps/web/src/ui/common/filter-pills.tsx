import type { ReactElement } from 'react';

/** Una fila de pills de filtro, de las que solo una esta activa. */
export function FilterPills<T extends string>({
  options,
  labels,
  selected,
  onSelect,
  label,
}: {
  options: readonly T[];
  labels: Readonly<Record<T, string>>;
  selected: T;
  onSelect: (option: T) => void;
  label: string;
}): ReactElement {
  return (
    <div aria-label={label} className="g-filters" role="tablist">
      {options.map((option) => (
        <button
          aria-selected={option === selected}
          className="g-filter"
          data-active={option === selected}
          key={option}
          onClick={() => {
            onSelect(option);
          }}
          role="tab"
          type="button"
        >
          {labels[option]}
        </button>
      ))}
    </div>
  );
}
