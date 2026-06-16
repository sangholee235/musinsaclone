import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getProduct } from '../api/products'
import { getReviews, getRating, type Review } from '../api/reviews'
import { useCartStore } from '../store/useCartStore'
import { useAuthStore } from '../store/useAuthStore'
import client from '../api/client'
import { getRecentlyViewed, addRecentlyViewed, type RecentProduct } from '../lib/recentlyViewed'
import styles from './ProductDetailPage.module.css'

const stars = (rating: number) => '★★★★★'.slice(0, rating) + '☆☆☆☆☆'.slice(0, 5 - rating)
const maskName = (name: string) =>
  name.length <= 1 ? name : name[0] + '*'.repeat(name.length - 1)

interface Option { id: number; size: string; color: string; stock: number; extraPrice: number }
interface Image { id: number; url: string; isMain: boolean }
interface ProductDetail {
  id: number; brandName: string; name: string; description: string
  price: number; discountRate: number; discountedPrice: number
  images: Image[]; options: Option[]
}

export default function ProductDetailPage() {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthStore()
  const [product, setProduct] = useState<ProductDetail | null>(null)
  const [selectedOption, setSelectedOption] = useState<Option | null>(null)
  const [quantity, setQuantity] = useState(1)
  const [mainImage, setMainImage] = useState('')
  const [isWishlisted, setIsWishlisted] = useState(false)
  const [toast, setToast] = useState('')
  const [reviews, setReviews] = useState<Review[]>([])
  const [avgRating, setAvgRating] = useState(0)
  const [reviewCount, setReviewCount] = useState(0)
  const [recent, setRecent] = useState<RecentProduct[]>([])
  const addItem = useCartStore((s) => s.addItem)

  useEffect(() => {
    getProduct(Number(productId)).then((res) => {
      const data = res.data.data
      setProduct(data)
      const main = data.images.find((i: Image) => i.isMain) ?? data.images[0]
      if (main) setMainImage(main.url)
      setRecent(getRecentlyViewed().filter((x: RecentProduct) => x.id !== data.id))
      addRecentlyViewed({
        id: data.id, brandName: data.brandName, name: data.name,
        discountedPrice: data.discountedPrice, imageUrl: main?.url ?? null,
      })
    })
    getRating(Number(productId)).then((res) => setAvgRating(res.data.data.averageRating ?? 0))
    getReviews(Number(productId)).then((res) => {
      setReviews(res.data.data.content ?? [])
      setReviewCount(res.data.data.totalElements ?? 0)
    })
  }, [productId])

  const showToast = (msg: string) => {
    setToast(msg)
    setTimeout(() => setToast(''), 2500)
  }

  const handleAddToCart = async () => {
    if (!isAuthenticated) return navigate('/login')
    if (!selectedOption) return showToast('옵션을 선택해주세요.')
    await addItem(selectedOption.id, quantity)
    showToast('장바구니에 담았습니다.')
  }

  const handleBuyNow = async () => {
    if (!isAuthenticated) return navigate('/login')
    if (!selectedOption) return showToast('옵션을 선택해주세요.')
    await addItem(selectedOption.id, quantity)
    navigate('/cart')
  }

  const handleWishlist = async () => {
    if (!isAuthenticated) return navigate('/login')
    const res = await client.post(`/wishlist/${productId}`)
    setIsWishlisted(res.data.data.added)
    showToast(res.data.data.added ? '찜 목록에 추가했습니다.' : '찜 목록에서 제거했습니다.')
  }

  if (!product) return (
    <div className={styles.loading}>
      <div className={styles.spinner} />
    </div>
  )

  const unitPrice = product.discountedPrice + (selectedOption?.extraPrice ?? 0)

  return (
    <div className="container">
      <div className={styles.page}>
        {/* 이미지 섹션 */}
        <div className={styles.gallery}>
          <div className={styles.mainImg}>
            {mainImage
              ? <img src={mainImage} alt={product.name} />
              : <div className={styles.noImg} />}
          </div>
          {product.images.length > 1 && (
            <div className={styles.thumbs}>
              {product.images.map((img) => (
                <button key={img.id} className={`${styles.thumb} ${mainImage === img.url ? styles.activeThumb : ''}`}
                  onClick={() => setMainImage(img.url)}>
                  <img src={img.url} alt="" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* 상품 정보 */}
        <div className={styles.info}>
          <p className={styles.brand}>{product.brandName}</p>
          <h1 className={styles.name}>{product.name}</h1>

          <div className={styles.priceSection}>
            {product.discountRate > 0 && (
              <div className={styles.originalRow}>
                <span className={styles.originalPrice}>{product.price.toLocaleString()}원</span>
                <span className={styles.discountRate}>{product.discountRate}% OFF</span>
              </div>
            )}
            <span className={styles.salePrice}>{product.discountedPrice.toLocaleString()}원</span>
          </div>

          <div className={styles.divider} />

          {/* 옵션 */}
          {product.options.length > 0 && (
            <div className={styles.optionSection}>
              <p className={styles.optionLabel}>옵션 선택</p>
              <div className={styles.optionList}>
                {product.options.map((opt) => (
                  <button key={opt.id}
                    className={`${styles.optionBtn} ${selectedOption?.id === opt.id ? styles.selectedOption : ''} ${opt.stock === 0 ? styles.soldOut : ''}`}
                    onClick={() => opt.stock > 0 && setSelectedOption(opt)}
                    disabled={opt.stock === 0}>
                    <span>{opt.size}{opt.color ? ` / ${opt.color}` : ''}</span>
                    {opt.extraPrice > 0 && <span className={styles.extraPrice}>+{opt.extraPrice.toLocaleString()}원</span>}
                    {opt.stock === 0 && <span className={styles.soldOutLabel}>품절</span>}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* 수량 */}
          {selectedOption && (
            <div className={styles.quantitySection}>
              <div className={styles.quantityRow}>
                <div className={styles.quantityControl}>
                  <button onClick={() => setQuantity(Math.max(1, quantity - 1))} className={styles.qBtn}>−</button>
                  <span className={styles.qNum}>{quantity}</span>
                  <button onClick={() => setQuantity(Math.min(selectedOption.stock, quantity + 1))} className={styles.qBtn}>+</button>
                </div>
                <span className={styles.subtotal}>{(unitPrice * quantity).toLocaleString()}원</span>
              </div>
              <p className={styles.stockInfo}>재고 {selectedOption.stock}개</p>
            </div>
          )}

          <div className={styles.divider} />

          {/* 버튼 */}
          <div className={styles.actions}>
            <button className={styles.wishBtn} onClick={handleWishlist}>
              <svg width="20" height="20" viewBox="0 0 24 24" fill={isWishlisted ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.8">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
              </svg>
            </button>
            <button className={`${styles.actionBtn} ${styles.cartBtn}`} onClick={handleAddToCart}>장바구니</button>
            <button className={`${styles.actionBtn} ${styles.buyBtn}`} onClick={handleBuyNow}>바로 구매</button>
          </div>

          {/* 상품 설명 */}
          {product.description && (
            <div className={styles.desc}>
              <p>{product.description}</p>
            </div>
          )}
        </div>
      </div>

      {/* 리뷰 */}
      <section className={styles.reviews}>
        <div className={styles.reviewHead}>
          <h2 className={styles.reviewTitle}>리뷰 <span className={styles.reviewCount}>{reviewCount}</span></h2>
          {reviewCount > 0 && (
            <div className={styles.ratingSummary}>
              <span className={styles.ratingStars}>{stars(Math.round(avgRating))}</span>
              <span className={styles.ratingNum}>{avgRating.toFixed(1)}</span>
            </div>
          )}
        </div>
        {reviews.length === 0 ? (
          <p className={styles.noReview}>아직 작성된 리뷰가 없습니다.<br />구매 후 첫 리뷰를 남겨보세요.</p>
        ) : (
          <ul className={styles.reviewList}>
            {reviews.map((r) => (
              <li key={r.reviewId} className={styles.reviewItem}>
                <div className={styles.reviewMeta}>
                  <span className={styles.reviewStars}>{stars(r.rating)}</span>
                  <span className={styles.reviewUser}>{maskName(r.userName)}</span>
                  <span className={styles.reviewDate}>{r.createdAt.slice(0, 10)}</span>
                </div>
                <p className={styles.reviewContent}>{r.content}</p>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* 최근 본 상품 */}
      {recent.length > 0 && (
        <section className={styles.recent}>
          <h2 className={styles.recentTitle}>최근 본 상품</h2>
          <div className={styles.recentStrip}>
            {recent.map((r) => (
              <button key={r.id} className={styles.recentCard} onClick={() => navigate(`/products/${r.id}`)}>
                <div className={styles.recentImg}>
                  {r.imageUrl ? <img src={r.imageUrl} alt={r.name} /> : <div className={styles.recentNoImg} />}
                </div>
                <p className={styles.recentBrand}>{r.brandName}</p>
                <p className={styles.recentName}>{r.name}</p>
                <p className={styles.recentPrice}>{r.discountedPrice.toLocaleString()}원</p>
              </button>
            ))}
          </div>
        </section>
      )}

      {/* 토스트 */}
      {toast && <div className={styles.toast}>{toast}</div>}
    </div>
  )
}
