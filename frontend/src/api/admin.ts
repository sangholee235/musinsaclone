import client from './client'

export interface AdminOrder {
  orderId: number
  userName: string
  userEmail: string
  finalPrice: number
  status: string
  createdAt: string
}

export const getAllOrders = (page = 0, size = 20) =>
  client.get('/admin/orders', { params: { page, size } })

export const updateOrderStatus = (orderId: number, status: string) =>
  client.patch(`/admin/orders/${orderId}/status`, { status })
