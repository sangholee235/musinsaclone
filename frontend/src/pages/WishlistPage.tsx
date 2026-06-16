import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client'
import styles from './WishlistPage.module.css'

interface WishItem {
  wishlistId: number; productId: number; productName: string; brandName: string; discountedPrice: number
}

export default function WishlistPage() {
  const [items, setItems] = useState<WishItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    client.get('/wishlist').then((res) => setItems(res.data.data.content ?? [])).finally(() => setLoading(false))
  }, [])

  const handleRemove = async (productId: number) => {
    await client.post(`/wishlist/${productId}`)
    setItems((prev) => prev.filter((i) => i.productId !== productId))
  }

  return (
    <div className="container">
      <div className={styles.page}>
        <h2 className={styles.title}>찜 목록 <span className={styles.count}>{items.length}</span></h2>

        {loading ? (
          <p className={styles.loading}>로딩 중...</p>
        ) : items.length === 0 ? (
          <div className={styles.empty}>
            <p>찜한 상품이 없습니다.</p>
            <Link to="/products" className="btn btn-primary" style={{ marginTop: 16 }}>상품 둘러보기</Link>
          </div>
        ) : (
          <div className={styles.grid}>
            {items.map((item) => (
              <div key={item.wishlistId} className={styles.card}>
                <Link to={`/products/${item.productId}`}>
                  <div className={styles.imgBox} />
                </Link>
                <div className={styles.info}>
                  <p className={styles.brand}>{item.brandName}</p>
                  <Link to={`/products/${item.productId}`} className={styles.name}>{item.productName}</Link>
                  <p className={styles.price}>{item.discountedPrice.toLocaleString()}원</p>
                </div>
                <button className={styles.removeBtn} onClick={() => handleRemove(item.productId)} title="찜 해제">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" stroke="none">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                  </svg>
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
