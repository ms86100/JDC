import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import './app-toast.css';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastItem {
  id: string;
  type: ToastType;
  message: string;
}

type PushToast = (type: ToastType, message: string) => void;

const ToastContext = createContext<PushToast | null>(null);

let externalPush: PushToast | null = null;

/** Call from non-React code (hooks) — same messages as before, styled in-app. */
export function pushAppToast(type: ToastType, message: string) {
  if (externalPush) externalPush(type, message);
}

export function useAppToast() {
  const push = useContext(ToastContext);
  return {
    success: (message: string) => push?.('success', message),
    error: (message: string) => push?.('error', message),
    info: (message: string) => push?.('info', message),
    warning: (message: string) => push?.('warning', message),
  };
}

export function AppToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const push = useCallback<PushToast>((type, message) => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    setToasts((prev) => [...prev.slice(-4), { id, type, message }]);
    window.setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 6000);
  }, []);

  useEffect(() => {
    externalPush = push;
    return () => {
      externalPush = null;
    };
  }, [push]);

  const dismiss = (id: string) => setToasts((prev) => prev.filter((t) => t.id !== id));

  return (
    <ToastContext.Provider value={push}>
      {children}
      <div className="sa-toast-stack" aria-live="polite" aria-relevant="additions">
        {toasts.map((t) => (
          <div key={t.id} className={`sa-toast sa-toast--${t.type}`} role="status">
            <span className="sa-toast__icon" aria-hidden="true">
              {t.type === 'success' && '✓'}
              {t.type === 'error' && '!'}
              {t.type === 'warning' && '⚠'}
              {t.type === 'info' && 'i'}
            </span>
            <p className="sa-toast__message">{t.message}</p>
            <button
              type="button"
              className="sa-toast__close"
              aria-label="Dismiss"
              onClick={() => dismiss(t.id)}
            >
              ×
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
