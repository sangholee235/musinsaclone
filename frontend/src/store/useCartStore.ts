import { create } from 'zustand'
import { getCart, addToCart, updateCartItem, removeCartItem } from '../api/cart'

interface CartItem {
  cartItemId: number
  productId: number
  productName: string
  brandName: string
  optionId: number
  size: string
  color: string
  price: number
  quantity: number
  totalPrice: number
  stock: number
  status: string
  available: boolean
}

interface CartState {
  items: CartItem[]
  totalPrice: number
  fetchCart: () => Promise<void>
  addItem: (productOptionId: number, quantity: number) => Promise<void>
  updateItem: (cartItemId: number, quantity: number) => Promise<void>
  removeItem: (cartItemId: number) => Promise<void>
}

export const useCartStore = create<CartState>((set, get) => ({
  items: [],
  totalPrice: 0,

  fetchCart: async () => {
    const res = await getCart()
    const items: CartItem[] = res.data.data
    set({ items, totalPrice: items.reduce((sum, i) => sum + i.totalPrice, 0) })
  },

  addItem: async (productOptionId, quantity) => {
    await addToCart(productOptionId, quantity)
    await get().fetchCart()
  },

  updateItem: async (cartItemId, quantity) => {
    await updateCartItem(cartItemId, quantity)
    await get().fetchCart()
  },

  removeItem: async (cartItemId) => {
    await removeCartItem(cartItemId)
    await get().fetchCart()
  },
}))
