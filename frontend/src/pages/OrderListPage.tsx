import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getOrders, cancelOrder } from '../api/orders'
import styles from './OrderListPage.module.css'

interface Order { orderId: number; status: string; finalPrice: number; createdAt: string }

const STATUS_LABEL: Record<string, string> = {
  PENDING: '결제대기', PAID: '결제완료', SHIPPING: '배송중',
  DELIVERED: '배송완료', CANCELLED: '취소됨',
}

const STATUS_COLOR: Record<string, string> = {
  PENDING: '#f59e0b', PAID: '#0066cc', SHIPPING: '#7c3aed',
  DELIVERED: '#16a34a', CANCELLED: '#a3a3a3',
}

export default function OrderListPage() {
  const navigate = useNavigate()
  const [orders, setOrders] = useState<Order[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getOrders().then((res) => setOrders(res.data.data.content ?? [])).finally(() => setLoading(false))
  }, [])

  const handleCancel = async (e: React.MouseEvent, orderId: number) => {
    e.stopPropagation()
    if (!confirm('주문을 취소하시겠습니까?')) return
    await cancelOrder(orderId)
    setOrders((prev) => prev.map((o) => o.orderId === orderId ? { ...o, status: 'CANCELLED' } : o))
  }

  return (
    <div className="container">
      <div className={styles.page}>
        <h2 className={styles.title}>주문 내역</h2>
        {loading ? (
          <div className={styles.loading}>로딩 중...</div>
        ) : orders.length === 0 ? (
          <div className={styles.empty}>주문 내역이 없습니다.</div>
        ) : (
          <div className={styles.list}>
            {orders.map((order) => (
              <div key={order.orderId} className={styles.item} onClick={() => navigate(`/orders/${order.orderId}`)}>
                <div className={styles.itemLeft}>
                  <p className={styles.date}>{order.createdAt.slice(0, 10)}</p>
                  <p className={styles.orderId}>주문번호 {order.orderId}</p>
                  <p className={styles.price}>{order.finalPrice.toLocaleString()}원</p>
                </div>
                <div className={styles.itemRight}>
                  <span className={styles.status} style={{ color: STATUS_COLOR[order.status] }}>
                    {STATUS_LABEL[order.status]}
                  </span>
                  {order.status === 'PENDING' && (
                    <button className={styles.cancelBtn} onClick={(e) => handleCancel(e, order.orderId)}>
                      취소
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
