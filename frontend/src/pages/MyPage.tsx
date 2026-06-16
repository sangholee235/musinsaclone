import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/useAuthStore'
import { logout } from '../api/auth'
import client from '../api/client'
import { getOrders } from '../api/orders'
import styles from './MyPage.module.css'

interface Order { orderId: number; status: string; finalPrice: number; createdAt: string }

const STATUS_LABEL: Record<string, string> = {
  PENDING: '결제대기', PAID: '결제완료', SHIPPING: '배송중',
  DELIVERED: '배송완료', CANCELLED: '취소됨',
}

const STATUS_COLOR: Record<string, string> = {
  PENDING: '#f59e0b', PAID: '#0066cc', SHIPPING: '#7c3aed',
  DELIVERED: '#16a34a', CANCELLED: '#a3a3a3',
}

const MENU_ITEMS = [
  { label: '주문 내역', icon: '📦', path: '/orders' },
  { label: '찜 목록', icon: '♡', path: '/wishlist' },
  { label: '쿠폰', icon: '🏷️', path: '/coupons' },
  { label: '포인트', icon: '💰', path: '/points' },
  { label: '배송지', icon: '📍', path: '/mypage/addresses' },
  { label: '알림', icon: '🔔', path: '/notifications' },
]

export default function MyPage() {
  const navigate = useNavigate()
  const { user, setUser, logout: storeLogout } = useAuthStore()
  const [orders, setOrders] = useState<Order[]>([])

  useEffect(() => {
    client.get('/users/me').then((res) => setUser(res.data.data))
    getOrders(0, 5).then((res) => setOrders(res.data.data.content ?? []))
  }, [])

  const handleLogout = async () => {
    try { await logout() } catch {}
    storeLogout()
    navigate('/')
  }

  return (
    <div className="container">
      <div className={styles.page}>
        {/* 프로필 */}
        <div className={styles.profile}>
          <div className={styles.avatar}>{user?.name?.[0] ?? 'U'}</div>
          <div>
            <p className={styles.userName}>{user?.name}님</p>
            <p className={styles.userEmail}>{user?.email}</p>
          </div>
          <button onClick={handleLogout} className={styles.logoutBtn}>로그아웃</button>
        </div>

        {/* 포인트 */}
        <div className={styles.pointCard}>
          <span className={styles.pointLabel}>보유 포인트</span>
          <span className={styles.pointValue}>{(user?.point ?? 0).toLocaleString()}P</span>
        </div>

        {/* 메뉴 */}
        <div className={styles.menuGrid}>
          {MENU_ITEMS.map((m) => (
            <Link key={m.label} to={m.path} className={styles.menuItem}>
              <span className={styles.menuIcon}>{m.icon}</span>
              <span className={styles.menuLabel}>{m.label}</span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#a3a3a3" strokeWidth="2"><polyline points="9 18 15 12 9 6" /></svg>
            </Link>
          ))}
        </div>

        {/* 최근 주문 */}
        <div className={styles.section}>
          <div className={styles.sectionHeader}>
            <h3 className={styles.sectionTitle}>최근 주문</h3>
            <Link to="/orders" className={styles.sectionMore}>전체보기</Link>
          </div>
          {orders.length === 0 ? (
            <p className={styles.emptyText}>주문 내역이 없습니다.</p>
          ) : (
            <div className={styles.orderList}>
              {orders.map((order) => (
                <div key={order.orderId} className={styles.orderItem} onClick={() => navigate(`/orders/${order.orderId}`)}>
                  <div>
                    <p className={styles.orderDate}>{order.createdAt.slice(0, 10)}</p>
                    <p className={styles.orderPrice}>{order.finalPrice.toLocaleString()}원</p>
                  </div>
                  <span className={styles.orderStatus} style={{ color: STATUS_COLOR[order.status] }}>
                    {STATUS_LABEL[order.status]}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
