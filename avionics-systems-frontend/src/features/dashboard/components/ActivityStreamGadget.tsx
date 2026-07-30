import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { auditApi, AuditLogResponse } from '../../../api/serviceApi';

function formatAction(log: AuditLogResponse) {
  const who = log.username ?? log.userId ?? 'Someone';
  const action = log.action?.toLowerCase() ?? 'updated';
  const entity = log.entityType?.toLowerCase() ?? 'item';
  return `${who} ${action} ${entity} ${log.entityId?.slice(0, 8) ?? ''}`;
}

export default function ActivityStreamGadget() {
  const { data, isLoading } = useQuery({
    queryKey: ['gadget-activity-audit'],
    queryFn: async () => {
      const res = await auditApi.getLogs({ page: 0, size: 8 });
      return res.data?.content ?? [];
    },
  });

  const logs = data ?? [];

  return (
    <section className="jdc-gadget" aria-label="Activity Stream">
      <div className="jdc-gadget-header">
        <span>Activity Stream</span>
      </div>
      <div className="jdc-gadget-body">
        <p style={{ fontWeight: 600, margin: '0 0 8px' }}>Your Company Systems</p>
        {isLoading && <p style={{ color: 'var(--jdc-text-subtle)' }}>Loading activity…</p>}
        {logs.map((log) => (
          <div key={log.id} style={{ marginBottom: 12, fontSize: 13 }}>
            <span style={{ color: 'var(--jdc-text-subtle)' }}>
              {log.createdAt ? new Date(log.createdAt).toLocaleString() : 'Recently'}
            </span>
            <p style={{ margin: '4px 0' }}>
              {formatAction(log)}
              {log.entityType === 'ISSUE' && (
                <>
                  {' '}
                  <Link to={`/issues/${log.entityId}`}>View</Link>
                </>
              )}
            </p>
          </div>
        ))}
        {!isLoading && logs.length === 0 && (
          <p style={{ color: 'var(--jdc-text-subtle)' }}>No recent activity from audit log.</p>
        )}
      </div>
    </section>
  );
}
