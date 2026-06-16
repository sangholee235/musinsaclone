import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCartStore } from '../store/useCartStore'
import { createOrder } from '../api/orders'
import { getMyCoupons, type MyCoupon } from '../api/coupons'
import { getPointBalance } from '../api/points'
import client from '../api/client'
import styles from './OrderPage.module.css'

interface Address {
  id: number; name: string; recipient: string; phone: string
  zipcode: string; address1: string; address2: string; isDefault: boolean
}

const calcDiscount = (coupon: MyCoupon | undefined, total: number) => {
  if (!coupon) return 0
  if (total < coupon.minOrderPrice) return 0
  return coupon.discountType === 'RATE'
    ? Math.floor((total * coupon.discountValue) / 100)
    : coupon.discountValue
}

export default function OrderPage() {
  const navigate = useNavigate()
  const { items, totalPrice } = useCartStore()
  const [addresses, setAddresses] = useState<Address[]>([])
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null)
  const [coupons, setCoupons] = useState<MyCoupon[]>([])
  const [selectedCouponId, setSelectedCouponId] = useState<number | null>(null)
  const [balance, setBalance] = useState(0)
  const [pointInput, setPointInput] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    client.get('/users/me/addresses').then((res) => {
      const list: Address[] = res.data.data ?? []
      setAddresses(list)
      const def = list.find((a) => a.isDefault)
      setSelectedAddressId(def ? def.id : list[0]?.id ?? null)
    })
    getMyCoupons().then((res) =>
      setCoupons((res.data.data ?? []).filter((c: MyCoupon) => !c.used && !c.expired)))
    getPointBalance().then((res) => setBalance(res.data.data.balance ?? 0))
  }, [])

  const selectedCoupon = useMemo(
    () => coupons.find((c) => c.userCouponId === selectedCouponId),
    [coupons, selectedCouponId])

  const discount = calcDiscount(selectedCoupon, totalPrice)
  const maxPoint = Math.max(0, Math.min(balance, totalPrice - discount))
  const pointUsed = Math.min(Math.max(0, parseInt(pointInput || '0', 10) || 0), maxPoint)
  const finalPrice = Math.max(0, totalPrice - discount - pointUsed)

  const handleOrder = async () => {
    if (!selectedAddressId) return alert('배송지를 선택해주세요.')
    if (items.length === 0) return alert('장바구니가 비어있습니다.')
    setLoading(true)
    try {
      const res = await createOrder({
        addressId: selectedAddressId,
        items: items.map((i) => ({ productOptionId: i.optionId, quantity: i.quantity })),
        userCouponId: selectedCouponId ?? undefined,
        pointUsed,
      })
      navigate(`/orders/${res.data.data.orderId}`)
    } catch {
      alert('주문에 실패했습니다. 다시 시도해주세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container">
      <div className={styles.page}>
        <h2 className={styles.title}>주문/결제</h2>

        <div className={styles.layout}>
          <div className={styles.left}>
            {/* 배송지 */}
            <section className={styles.section}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h3 className={styles.sectionTitle}>배송지</h3>
                <button type="button" onClick={() => navigate('/mypage/addresses')}
                  style={{ background: 'none', border: 'none', fontSize: 13, color: '#a3a3a3', cursor: 'pointer' }}>
                  + 배송지 관리
                </button>
              </div>
              {addresses.length === 0 ? (
                <div className={styles.noAddress}>
                  <p>등록된 배송지가 없습니다.</p>
                  <button className="btn btn-outline" onClick={() => navigate('/mypage/addresses')}>배송지 추가</button>
                </div>
              ) : (
                <div className={styles.addressList}>
                  {addresses.map((addr) => (
                    <label key={addr.id} className={`${styles.addressCard} ${selectedAddressId === addr.id ? styles.selectedAddr : ''}`}>
                      <input type="radio" name="address" value={addr.id} checked={selectedAddressId === addr.id}
                        onChange={() => setSelectedAddressId(addr.id)} className={styles.radioInput} />
                      <div className={styles.addrInfo}>
                        <div className={styles.addrName}>
                          {addr.name}
                          {addr.isDefault && <span className={styles.defaultBadge}>기본</span>}
                        </div>
                        <p className={styles.addrDetail}>{addr.recipient} · {addr.phone}</p>
                        <p className={styles.addrDetail}>[{addr.zipcode}] {addr.address1} {addr.address2}</p>
                      </div>
                    </label>
                  ))}
                </div>
              )}
            </section>

            {/* 주문 상품 */}
            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>주문 상품 {items.length}개</h3>
              <div className={styles.orderItems}>
                {items.map((item) => (
                  <div key={item.cartItemId} className={styles.orderItem}>
                    <div className={styles.orderItemThumb} />
                    <div className={styles.orderItemInfo}>
                      <p className={styles.orderItemBrand}>{item.brandName}</p>
                      <p className={styles.orderItemName}>{item.productName}</p>
                      <p className={styles.orderItemOption}>{item.size} {item.color} · {item.quantity}개</p>
                    </div>
                    <span className={styles.orderItemPrice}>{item.totalPrice.toLocaleString()}원</span>
                  </div>
                ))}
              </div>
            </section>

            {/* 할인 */}
            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>쿠폰 / 포인트</h3>
              <div className={styles.discountField}>
                <label className={styles.fieldLabel}>쿠폰</label>
                <select className={styles.couponSelect}
                  value={selectedCouponId ?? ''}
                  onChange={(e) => setSelectedCouponId(e.target.value ? Number(e.target.value) : null)}>
                  <option value="">쿠폰 선택 안 함</option>
                  {coupons.map((c) => {
                    const usable = totalPrice >= c.minOrderPrice
                    return (
                      <option key={c.userCouponId} value={c.userCouponId} disabled={!usable}>
                        {c.name} ({c.discountType === 'RATE' ? `${c.discountValue}%` : `${c.discountValue.toLocaleString()}원`})
                        {usable ? '' : ` · ${c.minOrderPrice.toLocaleString()}원 이상`}
                      </option>
                    )
                  })}
                </select>
              </div>
              <div className={styles.discountField}>
                <label className={styles.fieldLabel}>포인트</label>
                <div className={styles.pointRow}>
                  <input className={styles.pointInput} type="number" min={0} max={maxPoint}
                    placeholder="0" value={pointInput}
                    onChange={(e) => setPointInput(e.target.value)} />
                  <button type="button" className={styles.pointMaxBtn}
                    onClick={() => setPointInput(String(maxPoint))}>전액사용</button>
                </div>
              </div>
              <p className={styles.pointHint}>보유 {balance.toLocaleString()}P · 최대 {maxPoint.toLocaleString()}P 사용 가능</p>
            </section>
          </div>

          {/* 결제 요약 */}
          <div className={styles.summary}>
            <h3 className={styles.sectionTitle}>결제 금액</h3>
            <div className={styles.summaryRows}>
              <div className={styles.summaryRow}>
                <span>상품 금액</span><span>{totalPrice.toLocaleString()}원</span>
              </div>
              <div className={styles.summaryRow}>
                <span>배송비</span><span className={styles.free}>무료</span>
              </div>
              <div className={styles.summaryRow}>
                <span>쿠폰 할인</span><span className={styles.discountAmt}>{discount > 0 ? `-${discount.toLocaleString()}` : 0}원</span>
              </div>
              <div className={styles.summaryRow}>
                <span>포인트 사용</span><span className={styles.discountAmt}>{pointUsed > 0 ? `-${pointUsed.toLocaleString()}` : 0}원</span>
              </div>
            </div>
            <div className={styles.totalRow}>
              <span>총 결제금액</span>
              <span className={styles.totalPrice}>{finalPrice.toLocaleString()}원</span>
            </div>
            <button className={`btn btn-primary btn-full ${styles.orderBtn}`} onClick={handleOrder} disabled={loading || items.length === 0}>
              {loading ? '처리 중...' : `${finalPrice.toLocaleString()}원 결제하기`}
            </button>
            <p className={styles.notice}>주문 내용을 확인했으며 결제에 동의합니다.</p>
          </div>
        </div>
      </div>
    </div>
  )
}
