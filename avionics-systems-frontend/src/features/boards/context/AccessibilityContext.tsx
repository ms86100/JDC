import React, { createContext, useContext, useState, useCallback } from 'react';

interface AccessibilityContextType {
  highContrast: boolean;
  setHighContrast: (value: boolean) => void;
  reducedMotion: boolean;
  setReducedMotion: (value: boolean) => void;
  fontSize: 'normal' | 'large' | 'xlarge';
  setFontSize: (value: 'normal' | 'large' | 'xlarge') => void;
  announcements: string[];
  announce: (message: string) => void;
  focusVisible: boolean;
  setFocusVisible: (value: boolean) => void;
}

const AccessibilityContext = createContext<AccessibilityContextType | null>(null);

export function AccessibilityProvider({ children }: { children: React.ReactNode }) {
  const [highContrast, setHighContrast] = useState(false);
  const [reducedMotion, setReducedMotion] = useState(false);
  const [fontSize, setFontSize] = useState<'normal' | 'large' | 'xlarge'>('normal');
  const [announcements, setAnnouncements] = useState<string[]>([]);
  const [focusVisible, setFocusVisible] = useState(true);

  const announce = useCallback((message: string) => {
    setAnnouncements((prev) => [...prev.slice(-10), message]);
    setTimeout(() => {
      setAnnouncements((prev) => prev.filter((a) => a !== message));
    }, 5000);
  }, []);

  return (
    <AccessibilityContext.Provider
      value={{
        highContrast,
        setHighContrast: (v) => {
          setHighContrast(v);
          announce(v ? 'High contrast mode enabled' : 'High contrast mode disabled');
        },
        reducedMotion,
        setReducedMotion: (v) => {
          setReducedMotion(v);
          document.documentElement.style.setProperty(
            '--motion-duration',
            v ? '0ms' : '200ms',
          );
          announce(v ? 'Reduced motion enabled' : 'Reduced motion disabled');
        },
        fontSize,
        setFontSize: (v) => {
          setFontSize(v);
          document.documentElement.style.setProperty('--font-scale', v === 'normal' ? '1' : v === 'large' ? '1.15' : '1.3');
          announce('Font size changed');
        },
        announcements,
        announce,
        focusVisible,
        setFocusVisible,
      }}
    >
      <div
        className={`sa-a11y-root ${highContrast ? 'sa-high-contrast' : ''}`}
        data-font-size={fontSize}
        data-reduced-motion={reducedMotion}
      >
        {children}
        <div
          role="status"
          aria-live="polite"
          aria-atomic="true"
          className="sa-sr-only"
        >
          {announcements[announcements.length - 1]}
        </div>
      </div>
    </AccessibilityContext.Provider>
  );
}

export function useAccessibility() {
  const context = useContext(AccessibilityContext);
  if (!context) {
    throw new Error('useAccessibility must be used within AccessibilityProvider');
  }
  return context;
}