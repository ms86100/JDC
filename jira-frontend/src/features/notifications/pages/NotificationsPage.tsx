import { useQuery } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';

interface Notification {
  id: string;
  type: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export default function NotificationsPage() {
  const { data: notifications, isLoading } = useQuery<Notification[]>({
    queryKey: ['notifications'],
    queryFn: async () => {
      try {
        const response = await apiClient.get('/notifications');
        return response.data || [];
      } catch {
        return [];
      }
    },
  });

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  };

  const getNotificationIcon = (type: string) => {
    switch (type?.toLowerCase()) {
      case 'issue':
        return '📋';
      case 'comment':
        return '💬';
      case 'assignment':
        return '👤';
      case 'status':
        return '🔄';
      case 'mention':
        return '@';
      default:
        return '🔔';
    }
  };

  const getNotificationClass = (type: string) => {
    switch (type?.toLowerCase()) {
      case 'issue':
        return 'ab-notification-issue';
      case 'comment':
        return 'ab-notification-comment';
      case 'assignment':
        return 'ab-notification-assignment';
      case 'status':
        return 'ab-notification-status';
      default:
        return 'ab-notification-default';
    }
  };

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  return (
    <div className="ab-notifications-page">
      <div className="ab-page-header">
        <div>
          <h1 className="ab-page-title">Notifications</h1>
          <p className="ab-page-subtitle">Stay updated with your project activity</p>
        </div>
      </div>

      <div className="ab-card">
        {notifications && notifications.length > 0 ? (
          <div className="ab-notifications-list">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                className={`ab-notification-item ${!notification.read ? 'ab-unread' : ''}`}
              >
                <div className={`ab-notification-icon ${getNotificationClass(notification.type)}`}>
                  {getNotificationIcon(notification.type)}
                </div>
                <div className="ab-notification-content">
                  <div className="ab-notification-title">{notification.title}</div>
                  <div className="ab-notification-message">{notification.message}</div>
                  <div className="ab-notification-time">{formatDate(notification.createdAt)}</div>
                </div>
                {!notification.read && <div className="ab-unread-indicator"></div>}
              </div>
            ))}
          </div>
        ) : (
          <div className="ab-empty-state">
            <div className="ab-empty-state-icon">🔔</div>
            <h3 className="ab-empty-state-title">No notifications</h3>
            <p className="ab-empty-state-description">
              You're all caught up! Check back later for updates.
            </p>
          </div>
        )}
      </div>

      <style>{`
        .ab-notifications-page {
          padding: var(--ab-spacing-lg);
          max-width: 800px;
          margin: 0 auto;
        }

        .ab-notifications-list {
          display: flex;
          flex-direction: column;
        }

        .ab-notification-item {
          display: flex;
          align-items: flex-start;
          gap: var(--ab-spacing-md);
          padding: var(--ab-spacing-md);
          border-bottom: 1px solid var(--ab-gray-100);
          transition: background var(--ab-transition-fast);
        }

        .ab-notification-item:last-child {
          border-bottom: none;
        }

        .ab-notification-item:hover {
          background: var(--ab-gray-50);
        }

        .ab-notification-item.ab-unread {
          background: var(--ab-primary-50);
        }

        .ab-notification-icon {
          width: 40px;
          height: 40px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: var(--ab-font-size-lg);
          flex-shrink: 0;
        }

        .ab-notification-issue {
          background: #e6f0ff;
          color: #0066ff;
        }

        .ab-notification-comment {
          background: #e6f7e6;
          color: #28a745;
        }

        .ab-notification-assignment {
          background: #fff4e6;
          color: #ff9200;
        }

        .ab-notification-status {
          background: #f3e6ff;
          color: #6f42c1;
        }

        .ab-notification-default {
          background: var(--ab-gray-100);
          color: var(--ab-gray-600);
        }

        .ab-notification-content {
          flex: 1;
          min-width: 0;
        }

        .ab-notification-title {
          font-size: var(--ab-font-size-sm);
          font-weight: 600;
          color: var(--ab-gray-800);
          margin-bottom: 2px;
        }

        .ab-notification-message {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-600);
          margin-bottom: 4px;
        }

        .ab-notification-time {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-400);
        }

        .ab-unread-indicator {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background: var(--ab-primary-500);
          flex-shrink: 0;
          margin-top: 4px;
        }
      `}</style>
    </div>
  );
}