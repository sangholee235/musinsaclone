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

// ===== 상품 관리 =====

export interface AdminProduct {
  id: number
  brandId: number
  brandName: string
  categoryId: number
  categoryName: string
  name: string
  description: string
  price: number
  discountRate: number
  discountedPrice: number
  status: string
  mainImageUrl: string | null
}

export interface MetaItem {
  id: number
  name: string
}

export interface ProductMeta {
  brands: MetaItem[]
  categories: MetaItem[]
}

export interface ProductInput {
  brandId: number
  categoryId: number
  name: string
  description: string
  price: number
  discountRate: number
  status: string
  imageUrl: string
}

export const getAdminProducts = (page = 0, size = 20) =>
  client.get('/admin/products', { params: { page, size } })

export const getProductMeta = () => client.get('/admin/products/meta')

export const createProduct = (input: ProductInput) =>
  client.post('/admin/products', input)

export const updateProduct = (id: number, input: ProductInput) =>
  client.put(`/admin/products/${id}`, input)

export const updateProductStatus = (id: number, status: string) =>
  client.patch(`/admin/products/${id}/status`, { status })

// ===== 상품 옵션 관리 =====

export interface AdminOption {
  id: number
  size: string
  color: string
  stock: number
  extraPrice: number
}

export interface OptionInput {
  size: string
  color: string
  stock: number
  extraPrice: number
}

export const getProductOptions = (productId: number) =>
  client.get(`/admin/products/${productId}/options`)

export const addProductOption = (productId: number, input: OptionInput) =>
  client.post(`/admin/products/${productId}/options`, input)

export const updateProductOption = (optionId: number, input: OptionInput) =>
  client.put(`/admin/options/${optionId}`, input)

export const deleteProductOption = (optionId: number) =>
  client.delete(`/admin/options/${optionId}`)
