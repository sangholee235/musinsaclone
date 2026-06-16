// 최근 본 상품을 localStorage 에 저장/조회한다. (백엔드 불필요)

export interface RecentProduct {
  id: number
  brandName: string
  name: string
  discountedPrice: number
  imageUrl: string | null
}

const KEY = 'recentlyViewed'
const MAX = 12

export function getRecentlyViewed(): RecentProduct[] {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as RecentProduct[]) : []
  } catch {
    return []
  }
}

export function addRecentlyViewed(item: RecentProduct) {
  try {
    const list = getRecentlyViewed().filter((x) => x.id !== item.id)
    list.unshift(item)
    localStorage.setItem(KEY, JSON.stringify(list.slice(0, MAX)))
  } catch {
    // localStorage 사용 불가 시 무시
  }
}
