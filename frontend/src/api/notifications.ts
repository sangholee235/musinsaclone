import client from './client'

export interface Notification {
  notificationId: number
  type: string
  message: string
  isRead: boolean
  createdAt: string
}

export const getNotifications = (page = 0, size = 20) =>
  client.get('/notifications', { params: { page, size } })
export const getUnreadCount = () => client.get('/notifications/unread-count')
export const readNotification = (id: number) => client.patch(`/notifications/${id}/read`)
export const readAllNotifications = () => client.patch('/notifications/read-all')
