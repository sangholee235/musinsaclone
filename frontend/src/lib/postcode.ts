// 다음(카카오) 우편번호 서비스 연동. 스크립트를 지연 로드하고 팝업을 띄운다.
// 외부 스크립트가 차단된 환경에서는 reject 되며, 호출부에서 수동 입력으로 폴백한다.

declare global {
  interface Window {
    daum?: { Postcode: new (opts: { oncomplete: (data: PostcodeData) => void }) => { open: () => void } }
  }
}

interface PostcodeData {
  zonecode: string
  roadAddress: string
  jibunAddress: string
}

const SCRIPT_SRC = 'https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js'
let loading: Promise<void> | null = null

function loadScript(): Promise<void> {
  if (window.daum?.Postcode) return Promise.resolve()
  if (loading) return loading
  loading = new Promise<void>((resolve, reject) => {
    const s = document.createElement('script')
    s.src = SCRIPT_SRC
    s.async = true
    s.onload = () => resolve()
    s.onerror = () => {
      loading = null
      reject(new Error('우편번호 서비스를 불러오지 못했습니다. 직접 입력해주세요.'))
    }
    document.head.appendChild(s)
  })
  return loading
}

export interface PostcodeResult {
  zipcode: string
  address1: string
}

export async function openPostcode(): Promise<PostcodeResult> {
  await loadScript()
  return new Promise<PostcodeResult>((resolve) => {
    new window.daum!.Postcode({
      oncomplete: (data) => {
        resolve({ zipcode: data.zonecode, address1: data.roadAddress || data.jibunAddress })
      },
    }).open()
  })
}
