import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { getOrder, cancelOrder } from '../api/orders'
import { preparePayment, confirmPayment } from '../api/payments'
import { createReview } from '../api/reviews'
import styles from './OrderDetailPage.module.css'

interface OrderItem {
  orderItemId: number; productId: number; productName: string
  size: string; color: string; quantity: number; price: number
}
interface OrderDetail {
  orderId: number; status: string; totalPrice: number
  discountPrice: number; pointUsed: number; shippingFee: number; finalPrice: number
  items: OrderItem[]
}

const STATUS_LABEL: Record<string, string> = {
  PENDING: '결제대기', PAID: '결제완료', SHIPPING: '배송중',
  DELIVERED: '배송완료', CANCELLED: '취소됨',
}
const STATUS_COLOR: Record<string, string> = {
  PENDING: '#f59e0b', PAID: '#0066cc', SHIPPING: '#7c3aed',
  DELIVERED: '#16a34a', CANCELLED: '#a3a3a3',
}

export default function OrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const navigate = useNavigate()
  const [order, setOrder] = useState<OrderDetail | null>(null)
  const [busy, setBusy] = useState(false)
  const [reviewing, setReviewing] = useState<number | null>(null)
  const [rating, setRating] = useState(5)
  const [content, setContent] = useState('')
  const [reviewed, setReviewed] = useState<Set<number>>(new Set())

  const load = () => getOrder(Number(orderId)).then((res) => setOrder(res.data.data))
  useEffect(() => { load() }, [orderId])

  const handlePay = async () => {
    if (!order) return
    setBusy(true)
    try {
      await preparePayment(order.orderId, 'CARD')
      await confirmPayment(order.orderId, `SIM-${Date.now()}`)
      await load()
    } catch {
      alert('결제에 실패했습니다.')
    } finally { setBusy(false) }
  }

  const handleCancel = async () => {
    const msg = order?.status === 'PAID'
      ? '주문을 취소하시겠습니까?\n결제가 취소되고 사용한 포인트·쿠폰이 환불됩니다.'
      : '주문을 취소하시겠습니까?'
    if (!order || !confirm(msg)) return
    setBusy(true)
    try {
      await cancelOrder(order.orderId)
      await load()
    } catch {
      alert('취소에 실패했습니다.')
    } finally { setBusy(false) }
  }

  const submitReview = async (orderItemId: number) => {
    if (!content.trim()) return alert('리뷰 내용을 입력해주세요.')
    try {
      await createReview({ orderItemId, rating, content: content.trim() })
      setReviewed((prev) => new Set(prev).add(orderItemId))
      setReviewing(null); setContent(''); setRating(5)
      alert('리뷰가 등록되었습니다.')
    } catch (e: any) {
      alert(e?.response?.data?.message ?? '리뷰 등록에 실패했습니다.')
    }
  }

  if (!order) return <div className={styles.loading}><div className={styles.spinner} /></div>

  const canReview = ['PAID', 'SHIPPING', 'DELIVERED'].includes(order.status)
  const isPending = order.status === 'PENDING'
  const isPaid = order.status === 'PAID'

  return (
    <div className="container">
      <div className={styles.page}>
        <div className={styles.head}>
          <h2 className={styles.title}>주문 상세</h2>
          <span className={styles.status} style={{ color: STATUS_COLOR[order.status] }}>
            {STATUS_LABEL[order.status]}
          </span>
        </div>
        <p className={styles.orderNo}>주문번호 #{order.orderId}</p>

        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>주문 상품 {order.items.length}개</h3>
          <div className={styles.items}>
            {order.items.map((item) => (
              <div key={item.orderItemId} className={styles.item}>
                <Link to={`/products/${item.productId}`} className={styles.itemThumb} />
                <div className={styles.itemInfo}>
                  <Link to={`/products/${item.productId}`} className={styles.itemName}>{item.productName}</Link>
                  <p className={styles.itemOption}>{item.size} {item.color} · {item.quantity}개</p>
                  <p className={styles.itemPrice}>{item.price.toLocaleString()}원</p>
                </div>
                {canReview && !reviewed.has(item.orderItemId) && (
                  <button className={styles.reviewBtn}
                    onClick={() => setReviewing(reviewing === item.orderItemId ? null : item.orderItemId)}>
                    리뷰 작성
                  </button>
                )}
                {reviewed.has(item.orderItemId) && <span className={styles.reviewDone}>작성완료</span>}

                {reviewing === item.orderItemId && (
                  <div className={styles.reviewForm}>
                    <div className={styles.starPick}>
                      {[1, 2, 3, 4, 5].map((n) => (
                        <button key={n} type="button"
                          className={`${styles.star} ${n <= rating ? styles.starOn : ''}`}
                          onClick={() => setRating(n)}>★</button>
                      ))}
                    </div>
                    <textarea className={styles.reviewText} placeholder="상품은 어떠셨나요?"
                      value={content} onChange={(e) => setContent(e.target.value)} rows={3} />
                    <div className={styles.reviewActions}>
                      <button className="btn btn-outline" onClick={() => setReviewing(null)}>취소</button>
                      <button className="btn btn-primary" onClick={() => submitReview(item.orderItemId)}>등록</button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </section>

        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>결제 정보</h3>
          <div className={styles.summary}>
            <div className={styles.row}><span>상품 금액</span><span>{order.totalPrice.toLocaleString()}원</span></div>
            <div className={styles.row}><span>쿠폰 할인</span><span>{order.discountPrice > 0 ? `-${order.discountPrice.toLocaleString()}` : 0}원</span></div>
            <div className={styles.row}><span>포인트 사용</span><span>{order.pointUsed > 0 ? `-${order.pointUsed.toLocaleString()}` : 0}원</span></div>
            <div className={styles.row}><span>배송비</span><span>{order.shippingFee > 0 ? `+${order.shippingFee.toLocaleString()}원` : '무료'}</span></div>
            <div className={styles.totalRow}><span>총 결제금액</span><span className={styles.total}>{order.finalPrice.toLocaleString()}원</span></div>
          </div>
        </section>

        {(isPending || isPaid) && (
          <div className={styles.actions}>
            <button className="btn btn-outline btn-full" onClick={handleCancel} disabled={busy}>
              {isPaid ? '주문 취소 / 환불' : '주문 취소'}
            </button>
            {isPending && (
              <button className="btn btn-primary btn-full" onClick={handlePay} disabled={busy}>
                {busy ? '처리 중...' : `${order.finalPrice.toLocaleString()}원 결제하기`}
              </button>
            )}
          </div>
        )}
        <button className={styles.backBtn} onClick={() => navigate('/orders')}>주문 목록으로</button>
      </div>
    </div>
  )
}
