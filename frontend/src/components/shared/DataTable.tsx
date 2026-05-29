import React from 'react';
import type { ReactNode } from 'react';
import type { TableColumn } from '../../types';

interface DataTableProps<T extends { id?: string | number }> {
  title: string;
  meta?: ReactNode;
  columns: TableColumn[];
  rows: T[];
  renderRow: (row: T, index: number) => ReactNode;
  emptyMessage?: string;
  headerActions?: ReactNode;
}

export default function DataTable<T extends { id?: string | number }>({
  title,
  meta,
  columns,
  rows,
  renderRow,
  emptyMessage = 'No data.',
  headerActions,
}: DataTableProps<T>) {
  const gridTemplate = columns.map(c => c.width ?? '1fr').join(' ');

  return (
    <div className="panel">
      <div className="phead">
        <div>
          <h2>{title}</h2>
          {meta && <div className="meta">{meta}</div>}
        </div>
        {headerActions}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: gridTemplate, padding: '0 20px 8px' }}>
        {columns.map((col, i) => (
          <div key={i} className={`gh${col.align === 'right' ? ' r' : ''}`}>
            {col.label}
          </div>
        ))}

        {rows.length === 0 ? (
          <div style={{ gridColumn: '1 / -1', padding: '24px 0', color: 'var(--ink-3)', fontSize: '13px' }}>
            {emptyMessage}
          </div>
        ) : (
          rows.map((row, i) => (
            <React.Fragment key={row.id ?? i}>
              {renderRow(row, i)}
            </React.Fragment>
          ))
        )}
      </div>
    </div>
  );
}
