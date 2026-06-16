import client from './client'

export interface Brand {
  id: number
  name: string
  logoUrl: string | null
  followerCount: number
}

export const getBrands = (page = 0, size = 100) =>
  client.get('/brands', { params: { page, size } })

export const getBrand = (brandId: number) => client.get(`/brands/${brandId}`)

export const toggleFollow = (brandId: number) => client.post(`/brands/${brandId}/follow`)
