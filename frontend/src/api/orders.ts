import client from './client'

export const createOrder = (data: {
  addressId: number
  items: { productOptionId: number; quantity: number }[]
  userCouponId?: number
  pointUsed?: number
}) => client.post('/orders', data)

export const getOrders = (page = 0, size = 10) =>
  client.get('/orders', { params: { page, size } })

export const getOrder = (orderId: number) => client.get(`/orders/${orderId}`)

export const cancelOrder = (orderId: number) => client.post(`/orders/${orderId}/cancel`)
