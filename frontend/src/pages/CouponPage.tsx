import { useEffect, useState } from 'react'
import {
  getClaimableCoupons, claimCoupon, getMyCoupons,
  type ClaimableCoupon, type MyCoupon,
} from '../api/coupons'
import styles from './CouponPage.module.css'

const discountText = (type: 'FIXED' | 'RATE', value: number) =>
  type === 'RATE' ? `${value}%` : `${value.toLocaleString()}원`

export default function CouponPage() {
  const [claimable, setClaimable] = useState<ClaimableCoupon[]>([])
  const [mine, setMine] = useState<MyCoupon[]>([])
  const [tab, setTab] = useState<'store' | 'my'>('store')
  const [loadingId, setLoadingId] = useState<number | null>(null)

  const load = () => {
    getClaimableCoupons().then((res) => setClaimable(res.data.data ?? []))
    getMyCoupons().then((res) => setMine(res.data.data ?? []))
  }

  useEffect(() => { load() }, [])

  const handleClaim = async (couponId: number) => {
    setLoadingId(couponId)
    try {
      await claimCoupon(couponId)
      load()
    } catch (e: any) {
      alert(e?.response?.data?.message ?? '쿠폰 발급에 실패했습니다.')
    } finally {
      setLoadingId(null)
    }
  }

  return (
    <div className="container">
      <div className={styles.page}>
        <h2 className={styles.title}>쿠폰</h2>

        <div className={styles.tabs}>
          <button className={`${styles.tab} ${tab === 'store' ? styles.active : ''}`} onClick={() => setTab('store')}>
            받을 수 있는 쿠폰 <span className={styles.count}>{claimable.length}</span>
          </button>
          <button className={`${styles.tab} ${tab === 'my' ? styles.active : ''}`} onClick={() => setTab('my')}>
            내 쿠폰함 <span className={styles.count}>{mine.filter((c) => !c.used && !c.expired).length}</span>
          </button>
        </div>

        {tab === 'store' && (
          claimable.length === 0 ? (
            <p className={styles.empty}>받을 수 있는 쿠폰이 없습니다.</p>
          ) : (
            <div className={styles.list}>
              {claimable.map((c) => (
                <div key={c.couponId} className={styles.coupon}>
                  <div className={styles.left}>
                    <p className={styles.discount}>{discountText(c.discountType, c.discountValue)} 할인</p>
                    <p className={styles.name}>{c.name}</p>
                    <p className={styles.cond}>{c.minOrderPrice.toLocaleString()}원 이상 구매 시 · ~{c.expiredAt.slice(0, 10)}</p>
                  </div>
                  <button className={styles.claimBtn} disabled={loadingId === c.couponId || c.soldOut}
                    onClick={() => handleClaim(c.couponId)}>
                    {c.soldOut ? '마감' : loadingId === c.couponId ? '...' : '받기'}
                  </button>
                </div>
              ))}
            </div>
          )
        )}

        {tab === 'my' && (
          mine.length === 0 ? (
            <p className={styles.empty}>보유한 쿠폰이 없습니다.</p>
          ) : (
            <div className={styles.list}>
              {mine.map((c) => {
                const disabled = c.used || c.expired
                return (
                  <div key={c.userCouponId} className={`${styles.coupon} ${disabled ? styles.disabled : ''}`}>
                    <div className={styles.left}>
                      <p className={styles.discount}>{discountText(c.discountType, c.discountValue)} 할인</p>
                      <p className={styles.name}>{c.name}</p>
                      <p className={styles.cond}>{c.minOrderPrice.toLocaleString()}원 이상 구매 시 · ~{c.expiredAt.slice(0, 10)}</p>
                    </div>
                    {disabled && (
                      <span className={styles.stamp}>{c.used ? '사용완료' : '기간만료'}</span>
                    )}
                  </div>
                )
              })}
            </div>
          )
        )}
      </div>
    </div>
  )
}
