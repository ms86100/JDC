import { useEffect, useState } from 'react';

const WEBSUDO_KEY = 'sa.websudo.until';

/** Jira DC temporary admin elevation banner (Airbus-styled). */
export default function WebsudoBanner() {
  const [active, setActive] = useState(false);
  const [expiresAt, setExpiresAt] = useState<number | null>(null);

  useEffect(() => {
    const check = () => {
      const raw = localStorage.getItem(WEBSUDO_KEY);
      if (!raw) {
        setActive(false);
        return;
      }
      const until = parseInt(raw, 10);
      if (Number.isNaN(until) || Date.now() > until) {
        localStorage.removeItem(WEBSUDO_KEY);
        setActive(false);
        return;
      }
      setExpiresAt(until);
      setActive(true);
    };
    check();
    const onChange = () => check();
    window.addEventListener('sa-websudo-change', onChange);
    const id = window.setInterval(check, 30_000);
    return () => {
      window.clearInterval(id);
      window.removeEventListener('sa-websudo-change', onChange);
    };
  }, []);

  if (!active) return null;

  const dismiss = () => {
    localStorage.removeItem(WEBSUDO_KEY);
    setActive(false);
  };

  const minsLeft = expiresAt ? Math.max(1, Math.ceil((expiresAt - Date.now()) / 60_000)) : 0;

  return (
    <div className="sa-websudo-banner" role="status">
      <span>
        You are temporarily in <strong>administrator mode</strong> (websudo). Elevated access expires in ~
        {minsLeft} min.
      </span>
      <button type="button" className="sa-websudo-dismiss" onClick={dismiss} aria-label="End administrator mode">
        End session
      </button>
    </div>
  );
}

/** Enable websudo for 60 minutes (call from admin UI). */
export function enableWebsudo(minutes = 60): void {
  localStorage.setItem(WEBSUDO_KEY, String(Date.now() + minutes * 60_000));
  window.dispatchEvent(new Event('sa-websudo-change'));
}
