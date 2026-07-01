/**
 * Systems and Avionics — Button primitive (Phase 1 stub).
 * Variants: primary | secondary | subtle | danger | link
 * Sizes:    sm | md
 * Consumes --sa-* tokens only. Existing .ab-btn-* CSS untouched.
 */
import React from 'react';

type Variant = 'primary' | 'secondary' | 'subtle' | 'danger' | 'link';
type Size = 'sm' | 'md';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  iconLeft?: React.ReactNode;
  iconRight?: React.ReactNode;
  loading?: boolean;
}

const baseStyle: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 'var(--sa-space-2)',
  fontFamily: 'var(--sa-font-sans)',
  fontWeight: 500,
  borderRadius: 'var(--sa-radius-sm)',
  border: '1px solid transparent',
  cursor: 'pointer',
  transition: 'background var(--sa-motion-fast), border-color var(--sa-motion-fast), color var(--sa-motion-fast)',
  whiteSpace: 'nowrap',
};

const sizeStyle: Record<Size, React.CSSProperties> = {
  sm: { fontSize: 'var(--sa-fs-sm)', padding: '4px 10px', minHeight: 28 },
  md: { fontSize: 'var(--sa-fs-base)', padding: '6px 14px', minHeight: 32 },
};

const variantStyle: Record<Variant, React.CSSProperties> = {
  primary:   { background: 'var(--sa-brand-500)', color: 'var(--sa-n0)' },
  secondary: { background: 'var(--sa-n0)',        color: 'var(--sa-n800)', borderColor: 'var(--sa-n300)' },
  subtle:    { background: 'transparent',         color: 'var(--sa-n700)' },
  danger:    { background: 'var(--sa-danger-500)',color: 'var(--sa-n0)' },
  link:      { background: 'transparent',         color: 'var(--sa-brand-600)', padding: 0, minHeight: 0 },
};

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = 'primary', size = 'md', iconLeft, iconRight, loading, style, children, disabled, ...rest }, ref) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      style={{ ...baseStyle, ...sizeStyle[size], ...variantStyle[variant], opacity: (disabled || loading) ? 0.6 : 1, ...style }}
      {...rest}
    >
      {iconLeft}
      <span>{children}</span>
      {iconRight}
    </button>
  )
);
Button.displayName = 'Button';
