import client from './client'

export const getCart = () => client.get('/cart')

export const addToCart = (productOptionId: number, quantity: number) =>
  client.post('/cart', { productOptionId, quantity })

export const updateCartItem = (cartItemId: number, quantity: number) =>
  client.patch(`/cart/${cartItemId}`, null, { params: { quantity } })

export const removeCartItem = (cartItemId: number) => client.delete(`/cart/${cartItemId}`)
