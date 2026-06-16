import { useEffect } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useCartStore } from '../store/useCartStore'
import styles from './CartPage.module.css'

export default function CartPage() {
  const navigate = useNavigate()
  const { items, totalPrice, fetchCart, updateItem, removeItem } = useCartStore()

  useEffect(() => { fetchCart() }, [])

  if (items.length === 0) return (
    <div className={styles.empty}>
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d4d4d4" strokeWidth="1.5">
        <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" /><line x1="3" y1="6" x2="21" y2="6" /><path d="M16 10a4 4 0 0 1-8 0" />
      </svg>
      <p>장바구니가 비어있습니다.</p>
      <Link to="/products" className="btn btn-primary" style={{ marginTop: 20 }}>쇼핑 계속하기</Link>
    </div>
  )

  return (
    <div className="container">
      <div className={styles.page}>
        <h2 className={styles.title}>장바구니 <span className={styles.countBadge}>{items.length}</span></h2>

        <div className={styles.layout}>
          <div className={styles.itemList}>
            {items.map((item) => (
              <div key={item.cartItemId} className={styles.item}>
                <div className={styles.itemThumb}>
                  <div className={styles.thumbPlaceholder} />
                </div>
                <div className={styles.itemInfo}>
                  <p className={styles.itemBrand}>{item.brandName}</p>
                  <Link to={`/products/${item.productId}`} className={styles.itemName}>{item.productName}</Link>
                  <p className={styles.itemOption}>{item.size} {item.color}</p>
                  <div className={styles.itemBottom}>
                    <div className={styles.qtyControl}>
                      <button className={styles.qBtn} onClick={() => updateItem(item.cartItemId, item.quantity - 1)}>−</button>
                      <span>{item.quantity}</span>
                      <button className={styles.qBtn} onClick={() => updateItem(item.cartItemId, item.quantity + 1)}>+</button>
                    </div>
                    <span className={styles.itemPrice}>{item.totalPrice.toLocaleString()}원</span>
                  </div>
                </div>
                <button className={styles.removeBtn} onClick={() => removeItem(item.cartItemId)}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              </div>
            ))}
          </div>

          <div className={styles.summary}>
            <h3 className={styles.summaryTitle}>주문 요약</h3>
            <div className={styles.summaryRow}>
              <span>상품 금액</span>
              <span>{totalPrice.toLocaleString()}원</span>
            </div>
            <div className={styles.summaryRow}>
              <span>배송비</span>
              <span className={styles.free}>무료</span>
            </div>
            <div className={styles.summaryDivider} />
            <div className={`${styles.summaryRow} ${styles.totalRow}`}>
              <span>총 결제금액</span>
              <span>{totalPrice.toLocaleString()}원</span>
            </div>
            <button className={`btn btn-primary btn-full ${styles.orderBtn}`} onClick={() => navigate('/order')}>
              주문하기
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
