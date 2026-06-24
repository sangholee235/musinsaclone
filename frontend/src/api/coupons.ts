import client from './client'

export interface ClaimableCoupon {
  couponId: number
  name: string
  discountType: 'FIXED' | 'RATE'
  discountValue: number
  minOrderPrice: number
  expiredAt: string
  soldOut?: boolean
}

export interface MyCoupon extends ClaimableCoupon {
  userCouponId: number
  used: boolean
  expired: boolean
}

export const getClaimableCoupons = () => client.get('/coupons')
export const claimCoupon = (couponId: number) => client.post(`/coupons/${couponId}/claim`)
export const getMyCoupons = () => client.get('/coupons/my')
