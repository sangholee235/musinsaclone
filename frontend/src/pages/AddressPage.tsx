import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getAddresses, addAddress, deleteAddress, setDefaultAddress,
  type Address, type AddressInput,
} from '../api/users'
import { openPostcode } from '../lib/postcode'
import styles from './AddressPage.module.css'

const EMPTY: AddressInput = {
  name: '', recipient: '', phone: '', zipcode: '', address1: '', address2: '', isDefault: false,
}

export default function AddressPage() {
  const [addresses, setAddresses] = useState<Address[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<AddressInput>(EMPTY)
  const [saving, setSaving] = useState(false)

  const load = () => {
    setLoading(true)
    getAddresses()
      .then((res) => setAddresses(res.data.data ?? []))
      .finally(() => setLoading(false))
  }
  useEffect(load, [])

  const set = (k: keyof AddressInput, v: string | boolean) =>
    setForm((f) => ({ ...f, [k]: v }))

  const handleSearch = async () => {
    try {
      const r = await openPostcode()
      setForm((f) => ({ ...f, zipcode: r.zipcode, address1: r.address1 }))
    } catch (e) {
      alert((e as Error).message)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!form.name || !form.recipient || !form.phone || !form.zipcode || !form.address1) {
      return alert('필수 항목을 모두 입력해주세요.')
    }
    setSaving(true)
    try {
      await addAddress(form)
      setForm(EMPTY)
      setShowForm(false)
      load()
    } catch {
      alert('배송지 저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('이 배송지를 삭제할까요?')) return
    try {
      await deleteAddress(id)
      load()
    } catch {
      alert('삭제에 실패했습니다.')
    }
  }

  const handleSetDefault = async (id: number) => {
    try {
      await setDefaultAddress(id)
      load()
    } catch {
      alert('기본 배송지 설정에 실패했습니다.')
    }
  }

  return (
    <div className="container">
      <div className={styles.page}>
        <div className={styles.head}>
          <div>
            <Link to="/mypage" className={styles.back}>‹ 마이페이지</Link>
            <h2 className={styles.title}>배송지 관리</h2>
          </div>
          {!showForm && (
            <button className="btn btn-primary" onClick={() => setShowForm(true)}>+ 새 배송지</button>
          )}
        </div>

        {showForm && (
          <form className={styles.form} onSubmit={handleSubmit}>
            <div className={styles.field}>
              <label>배송지명 *</label>
              <input value={form.name} onChange={(e) => set('name', e.target.value)} placeholder="집, 회사 등" />
            </div>
            <div className={styles.row}>
              <div className={styles.field}>
                <label>받는 분 *</label>
                <input value={form.recipient} onChange={(e) => set('recipient', e.target.value)} />
              </div>
              <div className={styles.field}>
                <label>연락처 *</label>
                <input value={form.phone} onChange={(e) => set('phone', e.target.value)} placeholder="010-0000-0000" />
              </div>
            </div>
            <div className={styles.field}>
              <label>우편번호 *</label>
              <div className={styles.zipRow}>
                <input value={form.zipcode} onChange={(e) => set('zipcode', e.target.value)}
                  placeholder="주소 검색 또는 직접 입력" className={styles.zipInput} />
                <button type="button" className="btn btn-outline" onClick={handleSearch}>주소 검색</button>
              </div>
            </div>
            <div className={styles.field}>
              <label>기본 주소 *</label>
              <input value={form.address1} onChange={(e) => set('address1', e.target.value)} placeholder="도로명/지번 주소" />
            </div>
            <div className={styles.field}>
              <label>상세 주소</label>
              <input value={form.address2} onChange={(e) => set('address2', e.target.value)} placeholder="동/호수 등" />
            </div>
            <label className={styles.checkRow}>
              <input type="checkbox" checked={form.isDefault} onChange={(e) => set('isDefault', e.target.checked)} />
              기본 배송지로 설정
            </label>
            <div className={styles.formActions}>
              <button type="button" className="btn btn-outline" onClick={() => { setShowForm(false); setForm(EMPTY) }}>취소</button>
              <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? '저장 중...' : '저장'}</button>
            </div>
          </form>
        )}

        {loading ? (
          <p className={styles.empty}>불러오는 중...</p>
        ) : addresses.length === 0 && !showForm ? (
          <div className={styles.empty}>
            <p>등록된 배송지가 없습니다.</p>
            <p className={styles.emptySub}>새 배송지를 추가해보세요.</p>
          </div>
        ) : (
          <div className={styles.list}>
            {addresses.map((a) => (
              <div key={a.id} className={`${styles.card} ${a.isDefault ? styles.defaultCard : ''}`}>
                <div className={styles.cardHead}>
                  <span className={styles.cardName}>{a.name}</span>
                  {a.isDefault && <span className={styles.badge}>기본 배송지</span>}
                </div>
                <p className={styles.cardLine}>{a.recipient} · {a.phone}</p>
                <p className={styles.cardLine}>[{a.zipcode}] {a.address1} {a.address2}</p>
                <div className={styles.cardActions}>
                  {!a.isDefault && (
                    <button className={styles.linkBtn} onClick={() => handleSetDefault(a.id)}>기본으로 설정</button>
                  )}
                  <button className={styles.linkBtnDanger} onClick={() => handleDelete(a.id)}>삭제</button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
