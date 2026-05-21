import React from 'react';

interface MigrationPanelProps {
  title?: string;
  subtitle?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  'data-testid'?: string;
  noPadding?: boolean;
}

/** Standard panel chrome using --sa-* tokens. */
export default function MigrationPanel({
  title,
  subtitle,
  actions,
  children,
  className = '',
  'data-testid': testId,
  noPadding = false,
}: MigrationPanelProps) {
  return (
    <section className={`migration-panel ${className}`.trim()} data-testid={testId}>
      {(title || actions) && (
        <div className="migration-panel__header flex items-start justify-between gap-4">
          <div>
            {title && <h3 className="migration-panel__title">{title}</h3>}
            {subtitle && (
              <p style={{ margin: '4px 0 0', fontSize: 'var(--sa-fs-sm)', color: 'var(--sa-n600)' }}>
                {subtitle}
              </p>
            )}
          </div>
          {actions}
        </div>
      )}
      <div className={noPadding ? '' : 'migration-panel__body'}>{children}</div>
    </section>
  );
}
