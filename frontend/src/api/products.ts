import client from './client'

export const getProducts = (params?: {
  categoryId?: number
  brandId?: number
  minPrice?: number
  maxPrice?: number
  sort?: string
  sale?: boolean
  page?: number
  size?: number
}) => client.get('/products', { params })

export const getProduct = (productId: number) => client.get(`/products/${productId}`)

export const searchProducts = (keyword: string, page = 0, size = 20) =>
  client.get('/products/search', { params: { keyword, page, size } })
