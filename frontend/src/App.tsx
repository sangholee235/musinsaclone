import { useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Header from './components/Header'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import ProductListPage from './pages/ProductListPage'
import ProductDetailPage from './pages/ProductDetailPage'
import CartPage from './pages/CartPage'
import OrderPage from './pages/OrderPage'
import OrderListPage from './pages/OrderListPage'
import OrderDetailPage from './pages/OrderDetailPage'
import MyPage from './pages/MyPage'
import WishlistPage from './pages/WishlistPage'
import CouponPage from './pages/CouponPage'
import PointsPage from './pages/PointsPage'
import BrandsPage from './pages/BrandsPage'
import AddressPage from './pages/AddressPage'
import ProfilePage from './pages/ProfilePage'
import NotificationPage from './pages/NotificationPage'
import AdminOrdersPage from './pages/AdminOrdersPage'
import AdminProductsPage from './pages/AdminProductsPage'
import client from './api/client'
import { useAuthStore } from './store/useAuthStore'

function PrivateRoute({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const initialized = useAuthStore((s) => s.initialized)
  if (!initialized) return null
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

function AdminRoute({ children }: { children: React.ReactNode }) {
  const user = useAuthStore((s) => s.user)
  const initialized = useAuthStore((s) => s.initialized)
  if (!initialized) return null
  return user?.role === 'ADMIN' ? <>{children}</> : <Navigate to="/" replace />
}

function App() {
  const setUser = useAuthStore((s) => s.setUser)
  const setInitialized = useAuthStore((s) => s.setInitialized)

  // 새로고침 시 저장된 토큰으로 로그인 상태를 복원한다.
  useEffect(() => {
    if (!localStorage.getItem('accessToken')) {
      setInitialized()
      return
    }
    client.get('/users/me')
      .then((res) => setUser(res.data.data))
      .catch(() => setInitialized())
  }, [setUser, setInitialized])

  return (
    <BrowserRouter>
      <Header />
      <main style={{ minHeight: 'calc(100vh - 56px)' }}>
        <Routes>
          <Route path="/" element={<ProductListPage />} />
          <Route path="/products" element={<ProductListPage />} />
          <Route path="/products/:productId" element={<ProductDetailPage />} />
          <Route path="/brands" element={<BrandsPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/cart" element={<PrivateRoute><CartPage /></PrivateRoute>} />
          <Route path="/order" element={<PrivateRoute><OrderPage /></PrivateRoute>} />
          <Route path="/orders" element={<PrivateRoute><OrderListPage /></PrivateRoute>} />
          <Route path="/orders/:orderId" element={<PrivateRoute><OrderDetailPage /></PrivateRoute>} />
          <Route path="/mypage" element={<PrivateRoute><MyPage /></PrivateRoute>} />
          <Route path="/mypage/addresses" element={<PrivateRoute><AddressPage /></PrivateRoute>} />
          <Route path="/mypage/profile" element={<PrivateRoute><ProfilePage /></PrivateRoute>} />
          <Route path="/notifications" element={<PrivateRoute><NotificationPage /></PrivateRoute>} />
          <Route path="/wishlist" element={<PrivateRoute><WishlistPage /></PrivateRoute>} />
          <Route path="/coupons" element={<PrivateRoute><CouponPage /></PrivateRoute>} />
          <Route path="/points" element={<PrivateRoute><PointsPage /></PrivateRoute>} />
          <Route path="/admin" element={<AdminRoute><AdminOrdersPage /></AdminRoute>} />
          <Route path="/admin/products" element={<AdminRoute><AdminProductsPage /></AdminRoute>} />
        </Routes>
      </main>
    </BrowserRouter>
  )
}

export default App
