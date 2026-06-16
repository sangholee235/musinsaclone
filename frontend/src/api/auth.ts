import client from './client'

export const signup = (data: { email: string; password: string; name: string; phone?: string }) =>
  client.post('/auth/signup', data)

export const login = (data: { email: string; password: string }) =>
  client.post<{ data: { accessToken: string; refreshToken: string } }>('/auth/login', data)

export const logout = () => client.post('/auth/logout')
