import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProfile, updateProfile, changePassword } from '../api/users'
import { useAuthStore } from '../store/useAuthStore'
import styles from './ProfilePage.module.css'

export default function ProfilePage() {
  const setUser = useAuthStore((s) => s.setUser)
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [phone, setPhone] = useState('')
  const [savingProfile, setSavingProfile] = useState(false)

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [savingPw, setSavingPw] = useState(false)

  useEffect(() => {
    getProfile().then((res) => {
      const d = res.data.data
      setEmail(d.email)
      setName(d.name ?? '')
      setPhone(d.phone ?? '')
    })
  }, [])

  const saveProfile = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return alert('이름을 입력해주세요.')
    setSavingProfile(true)
    try {
      await updateProfile({ name: name.trim(), phone: phone.trim() })
      const res = await getProfile()
      setUser(res.data.data)
      alert('회원정보가 수정되었습니다.')
    } catch (err) {
      alert(errMsg(err) ?? '수정에 실패했습니다.')
    } finally {
      setSavingProfile(false)
    }
  }

  const savePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    if (newPassword.length < 8) return alert('새 비밀번호는 8자 이상이어야 합니다.')
    if (newPassword !== confirmPassword) return alert('새 비밀번호가 일치하지 않습니다.')
    setSavingPw(true)
    try {
      await changePassword({ currentPassword, newPassword })
      setCurrentPassword(''); setNewPassword(''); setConfirmPassword('')
      alert('비밀번호가 변경되었습니다.')
    } catch (err) {
      alert(errMsg(err) ?? '변경에 실패했습니다.')
    } finally {
      setSavingPw(false)
    }
  }

  return (
    <div className="container">
      <div className={styles.page}>
        <div className={styles.head}>
          <Link to="/mypage" className={styles.back}>‹ 마이페이지</Link>
          <h2 className={styles.title}>회원정보 수정</h2>
        </div>

        <form className={styles.card} onSubmit={saveProfile}>
          <h3 className={styles.cardTitle}>기본 정보</h3>
          <label className={styles.field}>
            <span>이메일</span>
            <input value={email} disabled className={styles.readonly} />
          </label>
          <label className={styles.field}>
            <span>이름</span>
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="이름" />
          </label>
          <label className={styles.field}>
            <span>전화번호</span>
            <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="010-0000-0000" />
          </label>
          <button type="submit" className={styles.saveBtn} disabled={savingProfile}>
            {savingProfile ? '저장 중...' : '정보 저장'}
          </button>
        </form>

        <form className={styles.card} onSubmit={savePassword}>
          <h3 className={styles.cardTitle}>비밀번호 변경</h3>
          <label className={styles.field}>
            <span>현재 비밀번호</span>
            <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} />
          </label>
          <label className={styles.field}>
            <span>새 비밀번호 (8자 이상)</span>
            <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
          </label>
          <label className={styles.field}>
            <span>새 비밀번호 확인</span>
            <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
          </label>
          <button type="submit" className={styles.saveBtn} disabled={savingPw}>
            {savingPw ? '변경 중...' : '비밀번호 변경'}
          </button>
        </form>
      </div>
    </div>
  )
}

function errMsg(err: unknown): string | undefined {
  return (err as { response?: { data?: { message?: string } } })?.response?.data?.message
}
