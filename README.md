# 무신사 클론 (Musinsa Clone)

학습용으로 제작한 패션 커머스 풀스택 클론 프로젝트입니다. 실제 서비스에서 쓰이는 기능과 인프라 구성을 최대한 반영했습니다.

## 기술 스택

### 백엔드
- **Java 17**, **Spring Boot 3.3**
- Spring Data JPA / Hibernate
- Spring Security + **JWT** (Access / Refresh 토큰)
- **MySQL 8** (운영 DB), **H2** (테스트 DB)
- **Redis** (캐시/세션, 선택적), **Kafka** (주문/알림 이벤트, 선택적), **Elasticsearch** (상품 검색, 선택적)
- **MinIO** (S3 호환 이미지 스토리지)
- **springdoc-openapi** (Swagger UI)

### 프론트엔드
- **React 19** + **TypeScript** + **Vite**
- **React Router 7**, **Zustand** (상태관리), **Axios** (JWT 인터셉터 / 자동 토큰 갱신)
- **CSS Modules** + CSS 변수 디자인 시스템 (반응형: 5→4→3→2 컬럼)

### 인프라
- **Docker Compose** 로 MySQL · Redis · Kafka · Elasticsearch · MinIO 일괄 구성

## 도메인 구성

```
auth · user · brand · category · product · cart · wishlist
coupon · point · order · payment · shipment · review · notification
```

- 공통: `ApiResponse<T>` 응답 래퍼, `BusinessException` + `GlobalExceptionHandler`, `BaseEntity`(생성/수정 시각 Auditing)
- Redis/Kafka/Elasticsearch 는 로컬에서 비활성화해도 기동되도록 설계 (`local` 프로파일에서 auto-config 제외, `Optional<KafkaTemplate>` 주입)

## 빠른 시작

### 1. 인프라 기동 (Docker)
```bash
docker-compose up -d mysql
# 필요 시: docker-compose up -d redis kafka elasticsearch minio
```
> MySQL 은 로컬 3306 충돌을 피하기 위해 **호스트 3307 → 컨테이너 3306** 으로 매핑되어 있습니다.

### 2. 백엔드 실행
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
# 또는 IntelliJ 에서 MusinsacloneApplication 실행 (기본 프로파일이 local)
```
→ http://localhost:8080

### 3. 프론트엔드 실행
```bash
cd frontend
npm install
npm run dev
```
→ http://localhost:5173

## 더미 계정 / 시드 데이터

앱 기동 시 `backend/src/main/resources/data.sql` 이 자동 적재됩니다 (모두 `INSERT IGNORE` 로 재시작 안전).

| 구분 | 이메일 | 비밀번호 | 권한 |
|------|--------|----------|------|
| 일반 | `test@test.com` | `test1234` | USER (50,000P, 배송지 2개, 포인트 이력 보유) |
| 관리자 | `admin@test.com` | `test1234` | ADMIN (헤더 '관리자' 메뉴 → `/admin` 주문 관리) |

상품 20종 · 브랜드 8개 · 카테고리 19개 · 쿠폰 2종 · 상품 이미지(placeholder) 포함.

## API 문서 (Swagger)

백엔드 기동 후:
- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

우측 상단 **Authorize** 에 로그인으로 발급받은 `accessToken` 을 넣으면 보안 API 도 바로 테스트할 수 있습니다. (총 40여 개 엔드포인트 자동 문서화)

## 주요 기능

- **인증**: 회원가입 / 로그인 / JWT 재발급 / 로그아웃
- **상품**: 목록 · 카테고리/브랜드/가격/세일 필터 · 정렬(신상품/가격순) · 검색 · 상세 · 옵션/재고
- **장바구니 · 위시리스트**
- **쿠폰**: 발급 가능 목록 · 다운로드 · 내 쿠폰함 (정액/정률, 최소주문금액, 만료 처리)
- **포인트**: 적립/사용 이력 · 잔액 · 결제 시 1% 자동 적립, 주문 사용/취소 환불 연동
- **주문/결제**: 쿠폰·포인트 적용 주문 → 결제(모의 PG) → 상태 전이 · 취소(재고/포인트 롤백)
- **리뷰**: 구매한 주문 항목 기준 작성(중복 방지) · 상품별 평점/목록
- **배송지**: 등록/삭제/기본배송지 설정(다음 우편번호 연동) · 주문 시 선택
- **알림**: 주문/결제/배송 상태 알림 · 헤더 안읽음 뱃지 (Kafka 활성 시 이벤트 기반, 비활성 시 in-process 생성으로 로컬에서도 동작)
- **관리자**: 전체 주문 조회 · 주문 상태 변경(→ 주문자에게 자동 알림) · `ADMIN` 역할 전용 라우트 가드

## 테스트

```bash
cd backend
./gradlew test
```
- H2 인메모리(`test` 프로파일)에서 실행 — 외부 인프라 불필요
- 도메인/서비스 단위 테스트(쿠폰·포인트·주문) + 컨텍스트 로드 검증 (**17개 통과**)

## 프로젝트 구조

```
musinsaclone/
├── backend/            # Spring Boot
│   └── src/main/java/com/musinsaclone/<도메인>/{controller,service,repository,entity}
├── frontend/           # React + Vite
│   └── src/{api,pages,components,store}
├── docker-compose.yml  # 인프라 일괄 구성
└── schema.sql          # 전체 DB 스키마 (참고용)
```

## 알려진 후속 과제 (TODO)

- 리뷰 이미지 업로드(MinIO 연동)
- 관리자(Admin) **상품** 관리 화면 (주문 관리는 구현 완료)
- Elasticsearch 기반 검색 고도화(현재는 DB LIKE 검색)
