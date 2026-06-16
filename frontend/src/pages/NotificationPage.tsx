import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getNotifications, readNotification, readAllNotifications, type Notification,
} from '../api/notifications'
import styles from './NotificationPage.module.css'

const TYPE_ICON: Record<string, string> = {
  ORDER_STATUS: '📦', SHIPMENT: '🚚', PROMOTION: '🎉', SYSTEM: '🔔',
}

function timeAgo(iso: string) {
  const diff = (Date.now() - new Date(iso).getTime()) / 1000
  if (diff < 60) return '방금 전'
  if (diff < 3600) return `${Math.floor(diff / 60)}분 전`
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`
  if (diff < 604800) return `${Math.floor(diff / 86400)}일 전`
  return iso.slice(0, 10)
}

export default function NotificationPage() {
  const [items, setItems] = useState<Notification[]>([])
  const [loading, setLoading] = useState(true)

  const load = () => {
    setLoading(true)
    getNotifications()
      .then((res) => setItems(res.data.data.content ?? []))
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  const handleRead = async (n: Notification) => {
    if (n.isRead) return
    try {
      await readNotification(n.notificationId)
      setItems((prev) => prev.map((x) => (x.notificationId === n.notificationId ? { ...x, isRead: true } : x)))
    } catch {}
  }

  const handleReadAll = async () => {
    try {
      await readAllNotifications()
      setItems((prev) => prev.map((x) => ({ ...x, isRead: true })))
    } catch {}
  }

  const unread = items.filter((x) => !x.isRead).length

  return (
    <div className="container">
      <div className={styles.page}>
        <div className={styles.head}>
          <div>
            <Link to="/mypage" className={styles.back}>‹ 마이페이지</Link>
            <h2 className={styles.title}>
              알림 {unread > 0 && <span className={styles.unreadCount}>{unread}</span>}
            </h2>
          </div>
          {unread > 0 && <button className={styles.readAllBtn} onClick={handleReadAll}>모두 읽음</button>}
        </div>

        {loading ? (
          <p className={styles.empty}>불러오는 중...</p>
        ) : items.length === 0 ? (
          <div className={styles.empty}>
            <p className={styles.emptyIcon}>🔔</p>
            <p>새로운 알림이 없습니다.</p>
          </div>
        ) : (
          <div className={styles.list}>
            {items.map((n) => (
              <div
                key={n.notificationId}
                className={`${styles.item} ${n.isRead ? '' : styles.itemUnread}`}
                onClick={() => handleRead(n)}
              >
                <span className={styles.icon}>{TYPE_ICON[n.type] ?? '🔔'}</span>
                <div className={styles.body}>
                  <p className={styles.message}>{n.message}</p>
                  <p className={styles.time}>{timeAgo(n.createdAt)}</p>
                </div>
                {!n.isRead && <span className={styles.dot} />}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
