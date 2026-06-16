import client from './client'

export interface PointHistory {
  id: number
  amount: number // 양수: 적립, 음수: 사용
  reason: string
  createdAt: string
}

export const getPointHistory = (page = 0, size = 20) =>
  client.get('/points', { params: { page, size } })

export const getPointBalance = () => client.get('/points/balance')
