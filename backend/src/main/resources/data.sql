-- 더미 계정 (비밀번호는 모두 test1234, BCrypt 해시)
INSERT IGNORE INTO users (id, email, password, name, phone, role, point, created_at, updated_at) VALUES
(1, 'test@test.com',  '$2a$10$2ePqEM.YrxzqXt14TpajTeUGXpnY3.PsUV7hj.iAdZybP7jee0bKC', '테스트', '010-1234-5678', 'USER',  50000, NOW(), NOW()),
(2, 'admin@test.com', '$2a$10$2ePqEM.YrxzqXt14TpajTeUGXpnY3.PsUV7hj.iAdZybP7jee0bKC', '관리자', '010-0000-0000', 'ADMIN', 0,     NOW(), NOW());

-- 더미 배송지
INSERT IGNORE INTO addresses (id, user_id, name, recipient, phone, zipcode, address1, address2, is_default) VALUES
(1, 1, '우리집', '테스트', '010-1234-5678', '04524', '서울특별시 중구 을지로 100', '101동 1001호', 1),
(2, 1, '회사',   '테스트', '010-1234-5678', '06164', '서울특별시 강남구 테헤란로 500', '20층',        0);

-- 더미 포인트 이력 (user 1)
INSERT IGNORE INTO point_histories (id, user_id, amount, reason, created_at) VALUES
(1, 1, 50000, '신규가입 적립',        NOW()),
(2, 1, 3000,  '리뷰 작성 적립',       NOW()),
(3, 1, -3000, '주문 결제 시 사용',    NOW());

-- 카테고리 (대분류)
INSERT IGNORE INTO categories (id, parent_id, name, sort_order) VALUES
(1, NULL, '상의', 1),
(2, NULL, '하의', 2),
(3, NULL, '아우터', 3),
(4, NULL, '신발', 4),
(5, NULL, '가방', 5),
(6, NULL, '액세서리', 6);

-- 카테고리 (소분류)
INSERT IGNORE INTO categories (id, parent_id, name, sort_order) VALUES
(11, 1, '반팔티', 1),
(12, 1, '긴팔티', 2),
(13, 1, '후드티', 3),
(14, 1, '맨투맨', 4),
(21, 2, '데님팬츠', 1),
(22, 2, '슬랙스', 2),
(23, 2, '조거팬츠', 3),
(31, 3, '자켓', 1),
(32, 3, '코트', 2),
(33, 3, '패딩', 3),
(41, 4, '스니커즈', 1),
(42, 4, '로퍼', 2),
(43, 4, '부츠', 3);

-- 브랜드
INSERT IGNORE INTO brands (id, name, logo_url, created_at) VALUES
(1, 'NIKE', NULL, NOW()),
(2, 'ADIDAS', NULL, NOW()),
(3, 'NEW BALANCE', NULL, NOW()),
(4, 'COVERNAT', NULL, NOW()),
(5, 'AJOBYAJO', NULL, NOW()),
(6, 'POTTERY', NULL, NOW()),
(7, 'MAISON MARGIELA', NULL, NOW()),
(8, 'STONE ISLAND', NULL, NOW());

-- 상품
INSERT IGNORE INTO products (id, brand_id, category_id, name, description, price, discount_rate, status, created_at, updated_at) VALUES
(1,  1, 11, 'Nike Sportswear 에센셜 반팔 티셔츠', '나이키 스포츠웨어의 클래식한 에센셜 티셔츠입니다.', 39000,  10, 'ON_SALE', NOW(), NOW()),
(2,  1, 41, 'Nike Air Force 1 07', '상징적인 농구화에서 영감을 받은 클래식 스니커즈.', 119000, 0,  'ON_SALE', NOW(), NOW()),
(3,  2, 11, 'Adidas Originals 트레포일 티셔츠', '트레포일 로고가 돋보이는 아디다스 오리지널스 티셔츠.', 49000,  20, 'ON_SALE', NOW(), NOW()),
(4,  2, 41, 'Adidas Samba OG', '70년대 풋볼 문화에서 탄생한 삼바 OG.', 139000, 0,  'ON_SALE', NOW(), NOW()),
(5,  3, 41, 'New Balance 993', '메이드 인 USA, 최고의 쿠셔닝을 자랑하는 993.', 259000, 0,  'ON_SALE', NOW(), NOW()),
(6,  3, 41, 'New Balance 550', '70년대 농구화에서 영감 받은 클린한 디자인.', 129000, 15, 'ON_SALE', NOW(), NOW()),
(7,  4, 14, 'Covernat 레귤러핏 크루넥 스웨트셔츠', '커버낫의 베이직 크루넥 맨투맨.', 69000,  0,  'ON_SALE', NOW(), NOW()),
(8,  4, 21, 'Covernat 데님 팬츠 와이드', '와이드 핏 데님 팬츠.', 89000,  0,  'ON_SALE', NOW(), NOW()),
(9,  5, 11, 'Ajobyajo OG Logo Tee', '아조바이아조의 시그니처 OG 로고 티셔츠.', 55000,  0,  'ON_SALE', NOW(), NOW()),
(10, 5, 13, 'Ajobyajo Pigment Hood', '피그먼트 워싱 후드티.', 99000,  0,  'ON_SALE', NOW(), NOW()),
(11, 6, 14, 'Pottery 스웨트셔츠', '포터리의 미니멀한 스웨트셔츠.', 128000, 0,  'ON_SALE', NOW(), NOW()),
(12, 6, 22, 'Pottery 울 슬랙스', '고급 울 소재의 테이퍼드 슬랙스.', 198000, 0,  'ON_SALE', NOW(), NOW()),
(13, 1, 31, 'Nike Windrunner 자켓', '바람막이 소재의 클래식 윈드러너 자켓.', 149000, 30, 'ON_SALE', NOW(), NOW()),
(14, 2, 31, 'Adidas Track Jacket', '아디다스 아이코닉 트랙 자켓.', 99000,  20, 'ON_SALE', NOW(), NOW()),
(15, 3, 41, 'New Balance 2002R', '2002R 러닝화 실루엣.', 169000, 0,  'ON_SALE', NOW(), NOW()),
(16, 4, 13, 'Covernat 피그먼트 후드 집업', '피그먼트 워싱 후드 집업 자켓.', 119000, 0,  'ON_SALE', NOW(), NOW()),
(17, 1, 23, 'Nike Tech Fleece 조거팬츠', '테크 플리스 소재의 조거팬츠.', 129000, 0,  'ON_SALE', NOW(), NOW()),
(18, 2, 23, 'Adidas Firebird 트랙팬츠', '아디다스 파이어버드 트랙팬츠.', 89000,  10, 'ON_SALE', NOW(), NOW()),
(19, 5, 21, 'Ajobyajo Wide Denim', '와이드 핏 데님.', 129000, 0,  'ON_SALE', NOW(), NOW()),
(20, 6, 32, 'Pottery 울 코트', '포터리 싱글 버튼 울 코트.', 498000, 0,  'ON_SALE', NOW(), NOW());

-- 상품 옵션 (사이즈별)
INSERT IGNORE INTO product_options (id, product_id, size, color, stock, extra_price) VALUES
-- 상품 1 (반팔티)
(1,  1, 'S',  '화이트', 50, 0),
(2,  1, 'M',  '화이트', 80, 0),
(3,  1, 'L',  '화이트', 60, 0),
(4,  1, 'XL', '화이트', 30, 0),
(5,  1, 'S',  '블랙',   40, 0),
(6,  1, 'M',  '블랙',   70, 0),
(7,  1, 'L',  '블랙',   55, 0),
-- 상품 2 (신발)
(8,  2, '250', '화이트', 20, 0),
(9,  2, '255', '화이트', 30, 0),
(10, 2, '260', '화이트', 25, 0),
(11, 2, '265', '화이트', 20, 0),
(12, 2, '270', '화이트', 15, 0),
-- 상품 3
(13, 3, 'S',  '화이트', 40, 0),
(14, 3, 'M',  '화이트', 60, 0),
(15, 3, 'L',  '화이트', 50, 0),
(16, 3, 'XL', '화이트', 20, 0),
-- 상품 4
(17, 4, '250', '화이트', 15, 0),
(18, 4, '255', '화이트', 20, 0),
(19, 4, '260', '화이트', 25, 0),
(20, 4, '265', '화이트', 20, 0),
-- 상품 5
(21, 5, '255', '그레이', 10, 0),
(22, 5, '260', '그레이', 15, 0),
(23, 5, '265', '그레이', 12, 0),
(24, 5, '270', '그레이', 8,  0),
-- 상품 6
(25, 6, '250', '크림',  20, 0),
(26, 6, '255', '크림',  25, 0),
(27, 6, '260', '크림',  20, 0),
(28, 6, '265', '크림',  15, 0),
-- 상품 7
(29, 7, 'S',  '챠콜',  30, 0),
(30, 7, 'M',  '챠콜',  50, 0),
(31, 7, 'L',  '챠콜',  40, 0),
(32, 7, 'XL', '챠콜',  20, 0),
-- 상품 8
(33, 8, '28', '인디고', 20, 0),
(34, 8, '30', '인디고', 30, 0),
(35, 8, '32', '인디고', 25, 0),
(36, 8, '34', '인디고', 15, 0),
-- 상품 9
(37, 9, 'S',  '블랙',  25, 0),
(38, 9, 'M',  '블랙',  40, 0),
(39, 9, 'L',  '블랙',  35, 0),
(40, 9, 'XL', '블랙',  15, 0),
-- 상품 10
(41, 10, 'S',  '블랙',  20, 0),
(42, 10, 'M',  '블랙',  35, 0),
(43, 10, 'L',  '블랙',  30, 0),
(44, 10, 'XL', '블랙',  10, 0),
-- 상품 11
(45, 11, 'S',  '오트밀', 15, 0),
(46, 11, 'M',  '오트밀', 25, 0),
(47, 11, 'L',  '오트밀', 20, 0),
-- 상품 12
(48, 12, '28', '베이지', 10, 0),
(49, 12, '30', '베이지', 15, 0),
(50, 12, '32', '베이지', 12, 0),
-- 상품 13
(51, 13, 'S',  '네이비', 30, 0),
(52, 13, 'M',  '네이비', 40, 0),
(53, 13, 'L',  '네이비', 35, 0),
(54, 13, 'XL', '네이비', 20, 0),
-- 상품 14
(55, 14, 'S',  '블랙',  25, 0),
(56, 14, 'M',  '블랙',  35, 0),
(57, 14, 'L',  '블랙',  30, 0),
-- 상품 15
(58, 15, '255', '실버', 15, 0),
(59, 15, '260', '실버', 20, 0),
(60, 15, '265', '실버', 18, 0),
(61, 15, '270', '실버', 10, 0),
-- 상품 16
(62, 16, 'S',  '블랙',  20, 0),
(63, 16, 'M',  '블랙',  30, 0),
(64, 16, 'L',  '블랙',  25, 0),
-- 상품 17
(65, 17, 'S',  '블랙',  25, 0),
(66, 17, 'M',  '블랙',  35, 0),
(67, 17, 'L',  '블랙',  30, 0),
(68, 17, 'XL', '블랙',  15, 0),
-- 상품 18
(69, 18, 'S',  '블랙',  20, 0),
(70, 18, 'M',  '블랙',  30, 0),
(71, 18, 'L',  '블랙',  25, 0),
-- 상품 19
(72, 19, '28', '블루', 15, 0),
(73, 19, '30', '블루', 20, 0),
(74, 19, '32', '블루', 18, 0),
-- 상품 20
(75, 20, 'S',  '카멜', 5,  0),
(76, 20, 'M',  '카멜', 8,  0),
(77, 20, 'L',  '카멜', 6,  0);

-- 상품 이미지 (picsum.photos 플레이스홀더)
INSERT IGNORE INTO product_images (id, product_id, url, is_main, sort_order) VALUES
(1,  1,  'https://picsum.photos/seed/prod1a/600/800',  1, 0),
(2,  1,  'https://picsum.photos/seed/prod1b/600/800',  0, 1),
(3,  2,  'https://picsum.photos/seed/prod2a/600/800',  1, 0),
(4,  2,  'https://picsum.photos/seed/prod2b/600/800',  0, 1),
(5,  3,  'https://picsum.photos/seed/prod3a/600/800',  1, 0),
(6,  3,  'https://picsum.photos/seed/prod3b/600/800',  0, 1),
(7,  4,  'https://picsum.photos/seed/prod4a/600/800',  1, 0),
(8,  4,  'https://picsum.photos/seed/prod4b/600/800',  0, 1),
(9,  5,  'https://picsum.photos/seed/prod5a/600/800',  1, 0),
(10, 5,  'https://picsum.photos/seed/prod5b/600/800',  0, 1),
(11, 6,  'https://picsum.photos/seed/prod6a/600/800',  1, 0),
(12, 6,  'https://picsum.photos/seed/prod6b/600/800',  0, 1),
(13, 7,  'https://picsum.photos/seed/prod7a/600/800',  1, 0),
(14, 7,  'https://picsum.photos/seed/prod7b/600/800',  0, 1),
(15, 8,  'https://picsum.photos/seed/prod8a/600/800',  1, 0),
(16, 8,  'https://picsum.photos/seed/prod8b/600/800',  0, 1),
(17, 9,  'https://picsum.photos/seed/prod9a/600/800',  1, 0),
(18, 9,  'https://picsum.photos/seed/prod9b/600/800',  0, 1),
(19, 10, 'https://picsum.photos/seed/prod10a/600/800', 1, 0),
(20, 10, 'https://picsum.photos/seed/prod10b/600/800', 0, 1),
(21, 11, 'https://picsum.photos/seed/prod11a/600/800', 1, 0),
(22, 11, 'https://picsum.photos/seed/prod11b/600/800', 0, 1),
(23, 12, 'https://picsum.photos/seed/prod12a/600/800', 1, 0),
(24, 12, 'https://picsum.photos/seed/prod12b/600/800', 0, 1),
(25, 13, 'https://picsum.photos/seed/prod13a/600/800', 1, 0),
(26, 13, 'https://picsum.photos/seed/prod13b/600/800', 0, 1),
(27, 14, 'https://picsum.photos/seed/prod14a/600/800', 1, 0),
(28, 14, 'https://picsum.photos/seed/prod14b/600/800', 0, 1),
(29, 15, 'https://picsum.photos/seed/prod15a/600/800', 1, 0),
(30, 15, 'https://picsum.photos/seed/prod15b/600/800', 0, 1),
(31, 16, 'https://picsum.photos/seed/prod16a/600/800', 1, 0),
(32, 16, 'https://picsum.photos/seed/prod16b/600/800', 0, 1),
(33, 17, 'https://picsum.photos/seed/prod17a/600/800', 1, 0),
(34, 17, 'https://picsum.photos/seed/prod17b/600/800', 0, 1),
(35, 18, 'https://picsum.photos/seed/prod18a/600/800', 1, 0),
(36, 18, 'https://picsum.photos/seed/prod18b/600/800', 0, 1),
(37, 19, 'https://picsum.photos/seed/prod19a/600/800', 1, 0),
(38, 19, 'https://picsum.photos/seed/prod19b/600/800', 0, 1),
(39, 20, 'https://picsum.photos/seed/prod20a/600/800', 1, 0),
(40, 20, 'https://picsum.photos/seed/prod20b/600/800', 0, 1);

-- 쿠폰
INSERT IGNORE INTO coupons (id, name, discount_type, discount_value, min_order_price, started_at, expired_at, total_quantity) VALUES
(1, '신규가입 10% 할인', 'RATE',  10, 10000,  NOW(), DATE_ADD(NOW(), INTERVAL 1 YEAR), NULL),
(2, '5,000원 할인 쿠폰',  'FIXED', 5000, 30000, NOW(), DATE_ADD(NOW(), INTERVAL 6 MONTH), 100),
(3, '선착순 한정 쿠폰',   'FIXED', 3000, 10000, NOW(), DATE_ADD(NOW(), INTERVAL 1 MONTH), 1);
