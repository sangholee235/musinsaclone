import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { signup } from '../api/auth'
import styles from './AuthPage.module.css'

export default function SignupPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '', name: '', phone: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (form.password.length < 8) return setError('비밀번호는 8자 이상이어야 합니다.')
    setLoading(true)
    try {
      await signup(form)
      navigate('/login')
    } catch (err: any) {
      setError(err.response?.data?.message ?? '회원가입에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <Link to="/" className={styles.logo}>MUSINSA</Link>
        <h1 className={styles.title}>회원가입</h1>

        {error && <div className={styles.errorBox}>{error}</div>}

        <form className={styles.form} onSubmit={handleSubmit}>
          <div className={styles.field}>
            <label className={styles.label}>이메일</label>
            <input type="email" className={styles.input} placeholder="이메일 주소"
              value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
          </div>
          <div className={styles.field}>
            <label className={styles.label}>비밀번호</label>
            <input type="password" className={styles.input} placeholder="8자 이상"
              value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
          </div>
          <div className={styles.field}>
            <label className={styles.label}>이름</label>
            <input type="text" className={styles.input} placeholder="이름"
              value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </div>
          <div className={styles.field}>
            <label className={styles.label}>휴대폰 <span className={styles.optional}>(선택)</span></label>
            <input type="tel" className={styles.input} placeholder="010-0000-0000"
              value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </div>
          <button type="submit" className={`btn btn-primary btn-full ${styles.submitBtn}`} disabled={loading}>
            {loading ? '처리 중...' : '가입하기'}
          </button>
        </form>

        <div className={styles.footer}>
          <span>이미 계정이 있으신가요?</span>
          <Link to="/login" className={styles.link}>로그인</Link>
        </div>
      </div>
    </div>
  )
}
