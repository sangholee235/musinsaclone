import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/useAuthStore'
import { useCartStore } from '../store/useCartStore'
import { logout } from '../api/auth'
import { getUnreadCount } from '../api/notifications'
import styles from './Header.module.css'

const NAV_LINKS = [
  { label: '신상품', path: '/products?sort=newest' },
  { label: '베스트', path: '/products?sort=best' },
  { label: '세일', path: '/products?sale=true' },
  { label: '브랜드', path: '/brands' },
]

export default function Header() {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated, user, logout: storeLogout } = useAuthStore()
  const cartCount = useCartStore((s) => s.items.length)
  const [keyword, setKeyword] = useState('')
  const [searchFocused, setSearchFocused] = useState(false)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const [unread, setUnread] = useState(0)
  const searchRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    setMobileMenuOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (!isAuthenticated) { setUnread(0); return }
    getUnreadCount().then((res) => setUnread(res.data.data.count ?? 0)).catch(() => {})
  }, [isAuthenticated, location.pathname])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    if (keyword.trim()) {
      navigate(`/products?keyword=${encodeURIComponent(keyword.trim())}`)
      setKeyword('')
      searchRef.current?.blur()
    }
  }

  const handleLogout = async () => {
    await logout()
    storeLogout()
    navigate('/')
  }

  return (
    <>
      <header className={styles.header}>
        <div className={styles.inner}>
          {/* 햄버거 (모바일) */}
          <button className={styles.hamburger} onClick={() => setMobileMenuOpen(!mobileMenuOpen)} aria-label="메뉴">
            <span /><span /><span />
          </button>

          <Link to="/" className={styles.logo}>MUSINSA</Link>

          {/* 검색 */}
          <form className={`${styles.searchForm} ${searchFocused ? styles.focused : ''}`} onSubmit={handleSearch}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
            </svg>
            <input
              ref={searchRef}
              type="text"
              placeholder="브랜드, 상품 검색"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onFocus={() => setSearchFocused(true)}
              onBlur={() => setSearchFocused(false)}
            />
          </form>

          {/* 아이콘 */}
          <div className={styles.icons}>
            {isAuthenticated ? (
              <>
                <Link to="/notifications" className={styles.icon} title="알림">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" /><path d="M13.73 21a2 2 0 0 1-3.46 0" />
                  </svg>
                  {unread > 0 && <span className={styles.badge}>{unread > 99 ? '99+' : unread}</span>}
                </Link>
                <Link to="/wishlist" className={styles.icon} title="찜">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                  </svg>
                </Link>
                <Link to="/cart" className={styles.icon} title="장바구니">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" /><line x1="3" y1="6" x2="21" y2="6" /><path d="M16 10a4 4 0 0 1-8 0" />
                  </svg>
                  {cartCount > 0 && <span className={styles.badge}>{cartCount}</span>}
                </Link>
                <Link to="/mypage" className={styles.icon} title="마이페이지">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
                  </svg>
                </Link>
                {user?.role === 'ADMIN' && <Link to="/admin" className={styles.textBtn}>관리자</Link>}
                <button onClick={handleLogout} className={styles.textBtn}>로그아웃</button>
              </>
            ) : (
              <>
                <Link to="/cart" className={styles.icon} title="장바구니">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                    <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" /><line x1="3" y1="6" x2="21" y2="6" /><path d="M16 10a4 4 0 0 1-8 0" />
                  </svg>
                </Link>
                <Link to="/login" className={styles.textBtn}>로그인</Link>
                <Link to="/signup" className={`${styles.textBtn} ${styles.signupBtn}`}>회원가입</Link>
              </>
            )}
          </div>
        </div>

        {/* 카테고리 nav */}
        <nav className={styles.nav}>
          <div className={styles.navInner}>
            {NAV_LINKS.map((l) => (
              <Link key={l.label} to={l.path} className={styles.navLink}>{l.label}</Link>
            ))}
          </div>
        </nav>
      </header>

      {/* 모바일 메뉴 오버레이 */}
      {mobileMenuOpen && (
        <div className={styles.mobileOverlay} onClick={() => setMobileMenuOpen(false)}>
          <div className={styles.mobileMenu} onClick={(e) => e.stopPropagation()}>
            <div className={styles.mobileMenuTop}>
              <span className={styles.logo}>MUSINSA</span>
              <button onClick={() => setMobileMenuOpen(false)} className={styles.closeBtn}>✕</button>
            </div>
            {NAV_LINKS.map((l) => (
              <Link key={l.label} to={l.path} className={styles.mobileNavLink}>{l.label}</Link>
            ))}
            <div className={styles.mobileDivider} />
            {isAuthenticated ? (
              <>
                <Link to="/mypage" className={styles.mobileNavLink}>마이페이지</Link>
                <Link to="/wishlist" className={styles.mobileNavLink}>찜 목록</Link>
                <Link to="/orders" className={styles.mobileNavLink}>주문 내역</Link>
                <button onClick={handleLogout} className={styles.mobileNavLink}>로그아웃</button>
              </>
            ) : (
              <>
                <Link to="/login" className={styles.mobileNavLink}>로그인</Link>
                <Link to="/signup" className={styles.mobileNavLink}>회원가입</Link>
              </>
            )}
          </div>
        </div>
      )}
    </>
  )
}
