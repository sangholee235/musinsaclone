import client from './client'

export const preparePayment = (orderId: number, method: string) =>
  client.post('/payments/prepare', { orderId, method })

export const confirmPayment = (orderId: number, pgTxId: string) =>
  client.post('/payments/confirm', { orderId, pgTxId })
