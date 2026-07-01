import React, { useState, useCallback } from 'react';

export type ScreenSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';

interface UseResponsiveBoardOptions {
  defaultLayout?: 'full' | 'compact' | 'minimal';
}

export function useResponsiveBoard({ defaultLayout = 'full' }: UseResponsiveBoardOptions = {}) {
  const [screenSize, setScreenSize] = useState<ScreenSize>('xl');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [layout, setLayout] = useState<'full' | 'compact' | 'minimal'>(defaultLayout);

  const updateScreenSize = useCallback(() => {
    const width = window.innerWidth;
    if (width < 640) setScreenSize('xs');
    else if (width < 768) setScreenSize('sm');
    else if (width < 1024) setScreenSize('md');
    else if (width < 1280) setScreenSize('lg');
    else if (width < 1536) setScreenSize('xl');
    else setScreenSize('2xl');
  }, []);

  React.useEffect(() => {
    updateScreenSize();
    window.addEventListener('resize', updateScreenSize);
    return () => window.removeEventListener('resize', updateScreenSize);
  }, [updateScreenSize]);

  const isMobile = screenSize === 'xs' || screenSize === 'sm';
  const isTablet = screenSize === 'md' || screenSize === 'lg';
  const isDesktop = screenSize === 'xl' || screenSize === '2xl';

  const getColumnCount = useCallback(() => {
    switch (screenSize) {
      case 'xs': return 1;
      case 'sm': return 2;
      case 'md': return 3;
      case 'lg': return 4;
      default: return 0;
    }
  }, [screenSize]);

  const shouldShowSidePanels = isDesktop;
  const shouldShowHeaders = !isMobile;
  const shouldUseTouchFriendly = isMobile;

  return {
    screenSize,
    setScreenSize,
    isMobile,
    isTablet,
    isDesktop,
    getColumnCount,
    shouldShowSidePanels,
    shouldShowHeaders,
    shouldUseTouchFriendly,
    isMobileMenuOpen,
    setIsMobileMenuOpen,
    layout,
    setLayout,
  };
}

export function useTouchGestures() {
  const [touchState, setTouchState] = useState<{
    startX: number;
    startY: number;
    direction: 'left' | 'right' | 'up' | 'down' | null;
  } | null>(null);

  const handleTouchStart = useCallback((e: React.TouchEvent) => {
    const touch = e.touches[0];
    setTouchState({
      startX: touch.clientX,
      startY: touch.clientY,
      direction: null,
    });
  }, []);

  const handleTouchMove = useCallback((e: React.TouchEvent) => {
    if (!touchState) return;

    const touch = e.touches[0];
    const deltaX = touch.clientX - touchState.startX;
    const deltaY = touch.clientY - touchState.startY;

    if (Math.abs(deltaX) > Math.abs(deltaY)) {
      if (Math.abs(deltaX) > 50) {
        setTouchState((prev) =>
          prev ? { ...prev, direction: deltaX > 0 ? 'right' : 'left' } : null,
        );
      }
    } else {
      if (Math.abs(deltaY) > 50) {
        setTouchState((prev) =>
          prev ? { ...prev, direction: deltaY > 0 ? 'down' : 'up' } : null,
        );
      }
    }
  }, [touchState]);

  const handleTouchEnd = useCallback(() => {
    setTouchState(null);
  }, []);

  return {
    touchState,
    handleTouchStart,
    handleTouchMove,
    handleTouchEnd,
  };
}