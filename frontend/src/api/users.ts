import client from './client'

export interface UserProfile {
  id: number
  email: string
  name: string
  phone: string
  point: number
}

export interface Address {
  id: number
  name: string
  recipient: string
  phone: string
  zipcode: string
  address1: string
  address2: string
  isDefault: boolean
}

export interface AddressInput {
  name: string
  recipient: string
  phone: string
  zipcode: string
  address1: string
  address2: string
  isDefault: boolean
}

export const getProfile = () => client.get('/users/me')
export const updateProfile = (data: { name: string; phone: string }) =>
  client.put('/users/me', data)
export const changePassword = (data: { currentPassword: string; newPassword: string }) =>
  client.put('/users/me/password', data)
export const getAddresses = () => client.get('/users/me/addresses')
export const addAddress = (data: AddressInput) => client.post('/users/me/addresses', data)
export const deleteAddress = (addressId: number) => client.delete(`/users/me/addresses/${addressId}`)
export const setDefaultAddress = (addressId: number) =>
  client.put(`/users/me/addresses/${addressId}/default`)
