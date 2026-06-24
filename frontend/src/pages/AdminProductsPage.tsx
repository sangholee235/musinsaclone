import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getAdminProducts, getProductMeta, createProduct, updateProduct, updateProductStatus,
  getProductOptions, addProductOption, updateProductOption, deleteProductOption,
  type AdminProduct, type ProductMeta, type ProductInput, type AdminOption, type OptionInput,
} from '../api/admin'
import styles from './AdminProductsPage.module.css'

const STATUSES = ['ON_SALE', 'SOLD_OUT', 'HIDDEN']
const STATUS_LABEL: Record<string, string> = {
  ON_SALE: '판매중', SOLD_OUT: '품절', HIDDEN: '숨김',
}
const STATUS_COLOR: Record<string, string> = {
  ON_SALE: '#16a34a', SOLD_OUT: '#f59e0b', HIDDEN: '#a3a3a3',
}

const EMPTY: ProductInput = {
  brandId: 0, categoryId: 0, name: '', description: '',
  price: 0, discountRate: 0, status: 'ON_SALE', imageUrl: '',
}

export default function AdminProductsPage() {
  const [products, setProducts] = useState<AdminProduct[]>([])
  const [meta, setMeta] = useState<ProductMeta | null>(null)
  const [loading, setLoading] = useState(true)
  const [savingId, setSavingId] = useState<number | null>(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editId, setEditId] = useState<number | null>(null)
  const [form, setForm] = useState<ProductInput>(EMPTY)
  const [submitting, setSubmitting] = useState(false)
  const [optionProduct, setOptionProduct] = useState<AdminProduct | null>(null)

  const load = () => {
    setLoading(true)
    getAdminProducts()
      .then((res) => setProducts(res.data.data.content ?? []))
      .finally(() => setLoading(false))
  }
  useEffect(() => {
    load()
    getProductMeta().then((res) => setMeta(res.data.data)).catch(() => {})
  }, [])

  const openCreate = () => {
    const first = meta
    setForm({
      ...EMPTY,
      brandId: first?.brands[0]?.id ?? 0,
      categoryId: first?.categories[0]?.id ?? 0,
    })
    setEditId(null)
    setModalOpen(true)
  }

  const openEdit = (p: AdminProduct) => {
    setForm({
      brandId: p.brandId, categoryId: p.categoryId, name: p.name,
      description: p.description ?? '', price: p.price, discountRate: p.discountRate,
      status: p.status, imageUrl: p.mainImageUrl ?? '',
    })
    setEditId(p.id)
    setModalOpen(true)
  }

  const handleStatus = async (id: number, status: string) => {
    setSavingId(id)
    try {
      await updateProductStatus(id, status)
      setProducts((prev) => prev.map((p) => (p.id === id ? { ...p, status } : p)))
    } catch {
      alert('상태 변경에 실패했습니다.')
    } finally {
      setSavingId(null)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!form.name.trim()) return alert('상품명을 입력해주세요.')
    if (!form.brandId || !form.categoryId) return alert('브랜드와 카테고리를 선택해주세요.')
    setSubmitting(true)
    try {
      if (editId == null) {
        await createProduct(form)
      } else {
        await updateProduct(editId, form)
      }
      setModalOpen(false)
      load()
    } catch {
      alert('저장에 실패했습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  const set = <K extends keyof ProductInput>(key: K, value: ProductInput[K]) =>
    setForm((f) => ({ ...f, [key]: value }))

  return (
    <div className="container">
      <div className={styles.page}>
        <div className={styles.head}>
          <div>
            <Link to="/" className={styles.back}>‹ 홈</Link>
            <h2 className={styles.title}>관리자 · 상품 관리</h2>
          </div>
          <div className={styles.headRight}>
            <Link to="/admin" className={styles.navLink}>주문 관리</Link>
            <button className={styles.addBtn} onClick={openCreate}>+ 상품 등록</button>
          </div>
        </div>

        {loading ? (
          <p className={styles.empty}>불러오는 중...</p>
        ) : products.length === 0 ? (
          <p className={styles.empty}>상품이 없습니다.</p>
        ) : (
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>이미지</th><th>상품</th><th>가격</th><th>할인</th><th>상태</th><th></th>
                </tr>
              </thead>
              <tbody>
                {products.map((p) => (
                  <tr key={p.id}>
                    <td>
                      <div className={styles.thumb}>
                        {p.mainImageUrl
                          ? <img src={p.mainImageUrl} alt="" />
                          : <div className={styles.noImg} />}
                      </div>
                    </td>
                    <td>
                      <div className={styles.pName}>{p.name}</div>
                      <div className={styles.pMeta}>{p.brandName} · {p.categoryName}</div>
                    </td>
                    <td className={styles.price}>
                      {p.discountedPrice.toLocaleString()}원
                      {p.discountRate > 0 && (
                        <span className={styles.original}>{p.price.toLocaleString()}원</span>
                      )}
                    </td>
                    <td className={styles.discount}>{p.discountRate > 0 ? `${p.discountRate}%` : '-'}</td>
                    <td>
                      <div className={styles.statusCell}>
                        <span className={styles.badge} style={{ color: STATUS_COLOR[p.status], borderColor: STATUS_COLOR[p.status] }}>
                          {STATUS_LABEL[p.status]}
                        </span>
                        <select
                          className={styles.select}
                          value={p.status}
                          disabled={savingId === p.id}
                          onChange={(e) => handleStatus(p.id, e.target.value)}
                        >
                          {STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
                        </select>
                      </div>
                    </td>
                    <td>
                      <div className={styles.rowActions}>
                        <button className={styles.editBtn} onClick={() => openEdit(p)}>수정</button>
                        <button className={styles.editBtn} onClick={() => setOptionProduct(p)}>옵션</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modalOpen && (
        <div className={styles.overlay} onClick={() => setModalOpen(false)}>
          <form className={styles.modal} onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
            <h3 className={styles.modalTitle}>{editId == null ? '상품 등록' : '상품 수정'}</h3>

            <div className={styles.row}>
              <label className={styles.field}>
                <span>브랜드</span>
                <select value={form.brandId} onChange={(e) => set('brandId', Number(e.target.value))}>
                  {meta?.brands.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
                </select>
              </label>
              <label className={styles.field}>
                <span>카테고리</span>
                <select value={form.categoryId} onChange={(e) => set('categoryId', Number(e.target.value))}>
                  {meta?.categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                </select>
              </label>
            </div>

            <label className={styles.field}>
              <span>상품명</span>
              <input value={form.name} onChange={(e) => set('name', e.target.value)} placeholder="상품명" />
            </label>

            <label className={styles.field}>
              <span>설명</span>
              <textarea value={form.description} onChange={(e) => set('description', e.target.value)} rows={3} placeholder="상품 설명" />
            </label>

            <div className={styles.row}>
              <label className={styles.field}>
                <span>가격(원)</span>
                <input type="number" min={0} value={form.price} onChange={(e) => set('price', Number(e.target.value))} />
              </label>
              <label className={styles.field}>
                <span>할인율(%)</span>
                <input type="number" min={0} max={100} value={form.discountRate} onChange={(e) => set('discountRate', Number(e.target.value))} />
              </label>
              <label className={styles.field}>
                <span>상태</span>
                <select value={form.status} onChange={(e) => set('status', e.target.value)}>
                  {STATUSES.map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
                </select>
              </label>
            </div>

            <label className={styles.field}>
              <span>대표 이미지 URL</span>
              <input value={form.imageUrl} onChange={(e) => set('imageUrl', e.target.value)} placeholder="https://..." />
            </label>

            <div className={styles.modalActions}>
              <button type="button" className={styles.cancelBtn} onClick={() => setModalOpen(false)}>취소</button>
              <button type="submit" className={styles.saveBtn} disabled={submitting}>
                {submitting ? '저장 중...' : '저장'}
              </button>
            </div>
          </form>
        </div>
      )}

      {optionProduct && (
        <OptionModal product={optionProduct} onClose={() => setOptionProduct(null)} />
      )}
    </div>
  )
}

const EMPTY_OPTION: OptionInput = { size: '', color: '', stock: 0, extraPrice: 0 }

function OptionModal({ product, onClose }: { product: AdminProduct; onClose: () => void }) {
  const [options, setOptions] = useState<AdminOption[]>([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [draft, setDraft] = useState<OptionInput>(EMPTY_OPTION)
  const [adding, setAdding] = useState(false)

  const load = () => {
    setLoading(true)
    getProductOptions(product.id)
      .then((res) => setOptions(res.data.data ?? []))
      .finally(() => setLoading(false))
  }
  useEffect(load, [product.id])

  const setField = (id: number, key: keyof AdminOption, value: string | number) =>
    setOptions((prev) => prev.map((o) => (o.id === id ? { ...o, [key]: value } : o)))

  const save = async (o: AdminOption) => {
    setBusyId(o.id)
    try {
      await updateProductOption(o.id, { size: o.size, color: o.color, stock: o.stock, extraPrice: o.extraPrice })
    } catch {
      alert('옵션 저장에 실패했습니다.')
    } finally {
      setBusyId(null)
    }
  }

  const remove = async (id: number) => {
    if (!confirm('이 옵션을 삭제할까요?')) return
    setBusyId(id)
    try {
      await deleteProductOption(id)
      setOptions((prev) => prev.filter((o) => o.id !== id))
    } catch (e) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      alert(msg ?? '옵션 삭제에 실패했습니다.')
    } finally {
      setBusyId(null)
    }
  }

  const add = async () => {
    if (!draft.size.trim() && !draft.color.trim()) return alert('사이즈 또는 색상을 입력해주세요.')
    setAdding(true)
    try {
      await addProductOption(product.id, draft)
      setDraft(EMPTY_OPTION)
      load()
    } catch {
      alert('옵션 추가에 실패했습니다.')
    } finally {
      setAdding(false)
    }
  }

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <h3 className={styles.modalTitle}>옵션 관리 · {product.name}</h3>

        {loading ? (
          <p className={styles.empty}>불러오는 중...</p>
        ) : (
          <div className={styles.optTable}>
            <div className={`${styles.optRow} ${styles.optHead}`}>
              <span>사이즈</span><span>색상</span><span>재고</span><span>추가금</span><span></span>
            </div>
            {options.length === 0 && <p className={styles.optEmpty}>등록된 옵션이 없습니다.</p>}
            {options.map((o) => (
              <div key={o.id} className={styles.optRow}>
                <input value={o.size} onChange={(e) => setField(o.id, 'size', e.target.value)} placeholder="-" />
                <input value={o.color} onChange={(e) => setField(o.id, 'color', e.target.value)} placeholder="-" />
                <input type="number" min={0} value={o.stock} onChange={(e) => setField(o.id, 'stock', Number(e.target.value))} />
                <input type="number" min={0} value={o.extraPrice} onChange={(e) => setField(o.id, 'extraPrice', Number(e.target.value))} />
                <div className={styles.optBtns}>
                  <button className={styles.optSave} disabled={busyId === o.id} onClick={() => save(o)}>저장</button>
                  <button className={styles.optDel} disabled={busyId === o.id} onClick={() => remove(o.id)}>삭제</button>
                </div>
              </div>
            ))}

            <div className={`${styles.optRow} ${styles.optAddRow}`}>
              <input value={draft.size} onChange={(e) => setDraft({ ...draft, size: e.target.value })} placeholder="사이즈" />
              <input value={draft.color} onChange={(e) => setDraft({ ...draft, color: e.target.value })} placeholder="색상" />
              <input type="number" min={0} value={draft.stock} onChange={(e) => setDraft({ ...draft, stock: Number(e.target.value) })} placeholder="재고" />
              <input type="number" min={0} value={draft.extraPrice} onChange={(e) => setDraft({ ...draft, extraPrice: Number(e.target.value) })} placeholder="추가금" />
              <div className={styles.optBtns}>
                <button className={styles.optSave} disabled={adding} onClick={add}>+ 추가</button>
              </div>
            </div>
          </div>
        )}

        <div className={styles.modalActions}>
          <button type="button" className={styles.cancelBtn} onClick={onClose}>닫기</button>
        </div>
      </div>
    </div>
  )
}
