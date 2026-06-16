import { useEffect, useState } from 'react'
import { getPointHistory, getPointBalance, type PointHistory } from '../api/points'
import styles from './PointsPage.module.css'

export default function PointsPage() {
  const [balance, setBalance] = useState(0)
  const [history, setHistory] = useState<PointHistory[]>([])

  useEffect(() => {
    getPointBalance().then((res) => setBalance(res.data.data.balance ?? 0))
    getPointHistory(0, 50).then((res) => setHistory(res.data.data.content ?? []))
  }, [])

  return (
    <div className="container">
      <div className={styles.page}>
        <h2 className={styles.title}>포인트</h2>

        <div className={styles.balanceCard}>
          <span className={styles.balanceLabel}>보유 포인트</span>
          <span className={styles.balanceValue}>{balance.toLocaleString()}<small>P</small></span>
        </div>

        <h3 className={styles.sectionTitle}>적립/사용 내역</h3>
        {history.length === 0 ? (
          <p className={styles.empty}>포인트 내역이 없습니다.</p>
        ) : (
          <ul className={styles.list}>
            {history.map((h) => (
              <li key={h.id} className={styles.item}>
                <div>
                  <p className={styles.reason}>{h.reason}</p>
                  <p className={styles.date}>{h.createdAt.slice(0, 10)}</p>
                </div>
                <span className={`${styles.amount} ${h.amount > 0 ? styles.plus : styles.minus}`}>
                  {h.amount > 0 ? '+' : ''}{h.amount.toLocaleString()}P
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
