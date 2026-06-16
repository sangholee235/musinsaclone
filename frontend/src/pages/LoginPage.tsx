import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { login } from '../api/auth'
import client from '../api/client'
import { useAuthStore } from '../store/useAuthStore'
import styles from './AuthPage.module.css'

export default function LoginPage() {
  const navigate = useNavigate()
  const { setUser, setTokens } = useAuthStore()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await login(form)
      const { accessToken, refreshToken } = res.data.data
      setTokens(accessToken, refreshToken)
      const profileRes = await client.get('/users/me')
      setUser(profileRes.data.data)
      navigate('/')
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <Link to="/" className={styles.logo}>MUSINSA</Link>
        <h1 className={styles.title}>로그인</h1>

        {error && <div className={styles.errorBox}>{error}</div>}

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.field}>
            <label className={styles.label}>이메일</label>
            <input type="email" className={styles.input} placeholder="이메일 주소 입력"
              value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
          </div>
          <div className={styles.field}>
            <label className={styles.label}>비밀번호</label>
            <input type="password" className={styles.input} placeholder="비밀번호 입력"
              value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
          </div>
          <button type="submit" className={`btn btn-primary btn-full ${styles.submitBtn}`} disabled={loading}>
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <div className={styles.footer}>
          <span>아직 계정이 없으신가요?</span>
          <Link to="/signup" className={styles.link}>회원가입</Link>
        </div>
      </div>
    </div>
  )
}
