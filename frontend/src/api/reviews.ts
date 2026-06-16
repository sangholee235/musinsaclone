import client from './client'

export interface Review {
  reviewId: number
  userName: string
  rating: number
  content: string
  createdAt: string
}

export const getReviews = (productId: number, page = 0, size = 10) =>
  client.get(`/reviews/products/${productId}`, { params: { page, size } })

export const getRating = (productId: number) =>
  client.get(`/reviews/products/${productId}/rating`)

export const createReview = (data: {
  productId: number
  orderItemId: number
  rating: number
  content: string
}) => client.post('/reviews', data)
