import { useEffect, useState } from 'react'
import { useSearchParams, Link } from 'react-router-dom'
import { getProducts, searchProducts } from '../api/products'
import styles from './ProductListPage.module.css'

interface Product {
  id: number
  brandName: string
  name: string
  price: number
  discountRate: number
  discountedPrice: number
  mainImageUrl: string | null
}

const SORT_OPTIONS = [
  { label: '추천순', value: 'recommend' },
  { label: '신상품순', value: 'newest' },
  { label: '인기순', value: 'popular' },
  { label: '낮은 가격순', value: 'priceAsc' },
  { label: '높은 가격순', value: 'priceDesc' },
]

export default function ProductListPage() {
  const [searchParams] = useSearchParams()
  const keyword = searchParams.get('keyword') ?? ''
  const categoryId = searchParams.get('categoryId')
  const brandId = searchParams.get('brandId')
  const urlSort = searchParams.get('sort')
  const sale = searchParams.get('sale') === 'true'

  const [products, setProducts] = useState<Product[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(false)
  const [sort, setSort] = useState(urlSort ?? 'recommend')
  const [viewMode, setViewMode] = useState<'grid' | 'list'>('grid')

  // 헤더 네비 등 URL 의 sort 파라미터가 바뀌면 드롭다운에 반영
  useEffect(() => {
    if (urlSort && SORT_OPTIONS.some((o) => o.value === urlSort)) setSort(urlSort)
  }, [urlSort])

  useEffect(() => { setPage(0) }, [keyword, categoryId, brandId, sort, sale])

  useEffect(() => {
    setLoading(true)
    const req = keyword
      ? searchProducts(keyword, page)
      : getProducts({
          categoryId: categoryId ? Number(categoryId) : undefined,
          brandId: brandId ? Number(brandId) : undefined,
          sort,
          sale: sale || undefined,
          page,
        })

    req.then((res) => {
      setProducts(res.data.data.content)
      setTotalPages(res.data.data.totalPages)
      setTotalElements(res.data.data.totalElements)
    }).finally(() => setLoading(false))
  }, [keyword, categoryId, brandId, sort, sale, page])

  return (
    <div className={styles.page}>
      <div className="container">
        {keyword && (
          <div className={styles.searchHeader}>
            <span className={styles.searchKeyword}>"{keyword}"</span>
            <span className={styles.searchCount}>{totalElements.toLocaleString()}개의 결과</span>
          </div>
        )}

        {sale && !keyword && (
          <div className={styles.searchHeader}>
            <span className={styles.searchKeyword}>🔥 세일</span>
            <span className={styles.searchCount}>{totalElements.toLocaleString()}개의 할인 상품</span>
          </div>
        )}

        {/* 툴바 */}
        <div className={styles.toolbar}>
          <span className={styles.count}>{totalElements.toLocaleString()}개 상품</span>
          <div className={styles.toolbarRight}>
            <select className={styles.sortSelect} value={sort} onChange={(e) => setSort(e.target.value)}>
              {SORT_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
            <div className={styles.viewToggle}>
              <button className={`${styles.viewBtn} ${viewMode === 'grid' ? styles.active : ''}`} onClick={() => setViewMode('grid')}>
                <GridIcon />
              </button>
              <button className={`${styles.viewBtn} ${viewMode === 'list' ? styles.active : ''}`} onClick={() => setViewMode('list')}>
                <ListIcon />
              </button>
            </div>
          </div>
        </div>

        {loading ? (
          <div className={styles.skeletonGrid}>
            {Array.from({ length: 20 }).map((_, i) => <SkeletonCard key={i} />)}
          </div>
        ) : products.length === 0 ? (
          <div className={styles.empty}>
            <p>검색 결과가 없습니다.</p>
            <Link to="/products" className="btn btn-outline" style={{ marginTop: 16 }}>전체 상품 보기</Link>
          </div>
        ) : (
          <div className={viewMode === 'grid' ? styles.grid : styles.list}>
            {products.map((p) => (
              <ProductCard key={p.id} product={p} viewMode={viewMode} />
            ))}
          </div>
        )}

        {/* 페이지네이션 */}
        {totalPages > 1 && (
          <div className={styles.pagination}>
            <button className={styles.pageBtn} onClick={() => setPage(Math.max(0, page - 1))} disabled={page === 0}>‹</button>
            {Array.from({ length: Math.min(totalPages, 10) }, (_, i) => {
              const p = Math.max(0, Math.min(page - 5, totalPages - 10)) + i
              return (
                <button key={p} className={`${styles.pageBtn} ${p === page ? styles.activePage : ''}`} onClick={() => setPage(p)}>
                  {p + 1}
                </button>
              )
            })}
            <button className={styles.pageBtn} onClick={() => setPage(Math.min(totalPages - 1, page + 1))} disabled={page === totalPages - 1}>›</button>
          </div>
        )}
      </div>
    </div>
  )
}

function ProductCard({ product: p, viewMode }: { product: Product; viewMode: 'grid' | 'list' }) {
  const [imgError, setImgError] = useState(false)
  if (viewMode === 'list') {
    return (
      <Link to={`/products/${p.id}`} className={styles.listCard}>
        <div className={styles.listImg}>
          {p.mainImageUrl && !imgError
            ? <img src={p.mainImageUrl} alt={p.name} onError={() => setImgError(true)} />
            : <div className={styles.noImg} />}
        </div>
        <div className={styles.listInfo}>
          <p className={styles.brand}>{p.brandName}</p>
          <p className={styles.name}>{p.name}</p>
          <div className={styles.priceRow}>
            {p.discountRate > 0 && <span className={styles.rate}>{p.discountRate}%</span>}
            <span className={styles.price}>{p.discountedPrice.toLocaleString()}원</span>
            {p.discountRate > 0 && <span className={styles.originalPrice}>{p.price.toLocaleString()}원</span>}
          </div>
        </div>
      </Link>
    )
  }
  return (
    <Link to={`/products/${p.id}`} className={styles.card}>
      <div className={styles.imgWrap}>
        {p.mainImageUrl && !imgError
          ? <img src={p.mainImageUrl} alt={p.name} onError={() => setImgError(true)} className={styles.img} />
          : <div className={styles.noImg} />}
        {p.discountRate >= 20 && <span className={styles.saleBadge}>SALE</span>}
      </div>
      <div className={styles.info}>
        <p className={styles.brand}>{p.brandName}</p>
        <p className={styles.name}>{p.name}</p>
        <div className={styles.priceRow}>
          {p.discountRate > 0 && <span className={styles.rate}>{p.discountRate}%</span>}
          <span className={styles.price}>{p.discountedPrice.toLocaleString()}원</span>
        </div>
      </div>
    </Link>
  )
}

function SkeletonCard() {
  return (
    <div className={styles.skeleton}>
      <div className={styles.skeletonImg} />
      <div className={styles.skeletonLine} style={{ width: '40%', marginTop: 10 }} />
      <div className={styles.skeletonLine} style={{ width: '80%', marginTop: 6 }} />
      <div className={styles.skeletonLine} style={{ width: '50%', marginTop: 6 }} />
    </div>
  )
}

function GridIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
      <rect x="1" y="1" width="6" height="6" /><rect x="9" y="1" width="6" height="6" />
      <rect x="1" y="9" width="6" height="6" /><rect x="9" y="9" width="6" height="6" />
    </svg>
  )
}

function ListIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
      <rect x="1" y="2" width="14" height="3" /><rect x="1" y="7" width="14" height="3" /><rect x="1" y="12" width="14" height="3" />
    </svg>
  )
}
