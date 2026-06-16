import { create } from 'zustand'

interface User {
  id: number
  email: string
  name: string
  point: number
  role?: string
}

interface AuthState {
  user: User | null
  isAuthenticated: boolean
  initialized: boolean
  setUser: (user: User) => void
  setTokens: (accessToken: string, refreshToken: string) => void
  setInitialized: () => void
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  initialized: false,
  setUser: (user) => set({ user, isAuthenticated: true, initialized: true }),
  setTokens: (accessToken, refreshToken) => {
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
  },
  setInitialized: () => set({ initialized: true }),
  logout: () => {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    set({ user: null, isAuthenticated: false })
  },
}))
