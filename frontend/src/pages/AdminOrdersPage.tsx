import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getAllOrders, updateOrderStatus, type AdminOrder } from '../api/admin'
import styles from './AdminOrdersPage.module.css'

const STATUSES = ['PENDING', 'PAID', 'SHIPPING', 'DELIVERED', 'CANCELLED']
const STATUS_LABEL: Record<string, string> = {
  PENDING: '결제대기', PAID: '결제완료', SHIPPING: '배송중', DELIVERED: '배송완료', CANCELLED: '취소됨',
}
const STATUS_COLOR: Record<string, string> = {
  PENDING: '#f59e0b', PAID: '#0066cc', SHIPPING: '#7c3aed', DELIVERED: '#16a34a', CANCELLED: '#a3a3a3',
}

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<AdminOrder[]>([])
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState<number | null>(null)

  const load = () => {
    setLoading(true)
    getAllOrders()
      .then((res) => setOrders(res.data.data.content ?? []))
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  const handleChange = async (orderId: number, status: string) => {
    setSavingId(orderId)
    try {
      await updateOrderStatus(orderId, status)
      setOrders((prev) => prev.map((o) => (o.orderId === orderId ? { ...o, status } : o)))
    } catch {
      alert('상태 변경에 실패했습니다.')
    } finally {
      setSavingId(null)
    }
  }

  return (
    <div className="container">
      <div className={styles.page}>
        <div className={styles.head}>
          <div>
            <Link to="/" className={styles.back}>‹ 홈</Link>
            <h2 className={styles.title}>관리자 · 주문 관리</h2>
          </div>
          <div className={styles.headRight}>
            <Link to="/admin/products" className={styles.navLink}>상품 관리</Link>
            <span className={styles.count}>총 {orders.length}건</span>
          </div>
        </div>

        {loading ? (
          <p className={styles.empty}>불러오는 중...</p>
        ) : orders.length === 0 ? (
          <p className={styles.empty}>주문이 없습니다.</p>
        ) : (
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>주문번호</th><th>주문자</th><th>결제금액</th><th>주문일</th><th>상태 변경</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((o) => (
                  <tr key={o.orderId}>
                    <td className={styles.oid}>#{o.orderId}</td>
                    <td>
                      <div className={styles.userName}>{o.userName}</div>
                      <div className={styles.userEmail}>{o.userEmail}</div>
                    </td>
                    <td className={styles.price}>{o.finalPrice.toLocaleString()}원</td>
                    <td className={styles.date}>{o.createdAt.slice(0, 10)}</td>
                    <td>
                      <div className={styles.statusCell}>
                        <span className={styles.badge} style={{ color: STATUS_COLOR[o.status], borderColor: STATUS_COLOR[o.status] }}>
                          {STATUS_LABEL[o.status]}
                        </span>
                        <select
                          className={styles.select}
                          value={o.status}
                          disabled={savingId === o.orderId}
                          onChange={(e) => handleChange(o.orderId, e.target.value)}
                        >
                          {STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
                        </select>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
