/**
 * Systems and Avionics — brand mark using Airbus design identity.
 * Uses the Airbus "A" logomark SVG from the design system icons.
 */
import React from 'react';

export interface AppBrandMarkProps {
  size?: number;
  showWordmark?: boolean;
  inverted?: boolean;
  className?: string;
}

export const AppBrandMark: React.FC<AppBrandMarkProps> = ({
  size = 24, showWordmark = true, inverted = false, className,
}) => {
  const fg = inverted ? '#ffffff' : 'var(--sa-brand-600, #063b9e)';
  return (
    <span className={`sa-brand-mark ${className ?? ''}`.trim()} style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
      <svg width={size} height={size} viewBox="0 0 24 24" role="img" aria-label="Systems and Avionics" className="sa-brand-icon" style={{ flexShrink: 0 }} fill="none">
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M4 2C2.89543 2 2 2.89543 2 4V20C2 21.1046 2.89543 22 4 22H20C21.1046 22 22 21.1046 22 20V4C22 2.89543 21.1046 2 20 2H4ZM3.23592 19L10.3745 5H13.4002L20.5388 19H16.6179L11.8448 9.41102H11.8019L9.67869 13.7154H12.5479L13.9755 16.6561H8.22777L7.07143 19H3.23592Z"
          fill={fg}
        />
      </svg>
      {showWordmark && (
        <span className={`sa-brand-wordmark ${inverted ? 'sa-brand-wordmark--inverted' : ''}`}>
          <span className="sa-brand-wordmark-full">Systems and Avionics</span>
          <span className="sa-brand-wordmark-short" aria-hidden="true">S&amp;A</span>
        </span>
      )}
    </span>
  );
};
