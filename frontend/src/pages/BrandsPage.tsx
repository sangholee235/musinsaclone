import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getBrands, type Brand } from '../api/brands'
import styles from './BrandsPage.module.css'

export default function BrandsPage() {
  const [brands, setBrands] = useState<Brand[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getBrands().then((res) => setBrands(res.data.data.content ?? []))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div className="container">
      <div className={styles.page}>
        <h2 className={styles.title}>브랜드</h2>
        <p className={styles.subtitle}>{brands.length}개 브랜드</p>

        {loading ? (
          <div className={styles.grid}>
            {Array.from({ length: 8 }).map((_, i) => <div key={i} className={styles.skeleton} />)}
          </div>
        ) : (
          <div className={styles.grid}>
            {brands.map((b) => (
              <Link key={b.id} to={`/products?brandId=${b.id}`} className={styles.card}>
                <div className={styles.logo}>
                  {b.logoUrl ? <img src={b.logoUrl} alt={b.name} /> : <span>{b.name[0]}</span>}
                </div>
                <p className={styles.name}>{b.name}</p>
                <p className={styles.followers}>팔로워 {b.followerCount.toLocaleString()}</p>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
