-- ================================
-- Musinsa Clone DB Schema (MySQL)
-- ================================

CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(20),
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    point      INT          NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE addresses (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    name       VARCHAR(100) NOT NULL,
    recipient  VARCHAR(100) NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    zipcode    VARCHAR(10)  NOT NULL,
    address1   VARCHAR(255) NOT NULL,
    address2   VARCHAR(255),
    is_default TINYINT(1)   NOT NULL DEFAULT 0
);

CREATE TABLE brands (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    logo_url   VARCHAR(500),
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE brand_follows (
    id         BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT   NOT NULL REFERENCES users(id),
    brand_id   BIGINT   NOT NULL REFERENCES brands(id),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_brand_follow (user_id, brand_id)
);

CREATE TABLE categories (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT       REFERENCES categories(id),
    name       VARCHAR(100) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    brand_id      BIGINT       NOT NULL REFERENCES brands(id),
    category_id   BIGINT       NOT NULL REFERENCES categories(id),
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    price         INT          NOT NULL,
    discount_rate INT          NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ON_SALE',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE product_images (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT       NOT NULL REFERENCES products(id),
    url        VARCHAR(500) NOT NULL,
    is_main    TINYINT(1)   NOT NULL DEFAULT 0,
    sort_order INT          NOT NULL DEFAULT 0
);

CREATE TABLE product_options (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT      NOT NULL REFERENCES products(id),
    size        VARCHAR(50),
    color       VARCHAR(50),
    stock       INT         NOT NULL DEFAULT 0,
    extra_price INT         NOT NULL DEFAULT 0
);

CREATE TABLE product_tags (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT       NOT NULL REFERENCES products(id),
    tag        VARCHAR(100) NOT NULL
);

CREATE TABLE wishlists (
    id         BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT   NOT NULL REFERENCES users(id),
    product_id BIGINT   NOT NULL REFERENCES products(id),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_wishlist (user_id, product_id)
);

CREATE TABLE cart_items (
    id                  BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT   NOT NULL REFERENCES users(id),
    product_option_id   BIGINT   NOT NULL REFERENCES product_options(id),
    quantity            INT      NOT NULL DEFAULT 1,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_cart_item (user_id, product_option_id)
);

CREATE TABLE coupons (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    discount_type   VARCHAR(20)  NOT NULL,
    discount_value  INT          NOT NULL,
    min_order_price INT          NOT NULL DEFAULT 0,
    started_at      DATETIME     NOT NULL,
    expired_at      DATETIME     NOT NULL
);

CREATE TABLE user_coupons (
    id        BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id   BIGINT   NOT NULL REFERENCES users(id),
    coupon_id BIGINT   NOT NULL REFERENCES coupons(id),
    is_used   TINYINT(1) NOT NULL DEFAULT 0,
    used_at   DATETIME,
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id),
    address_id      BIGINT      NOT NULL REFERENCES addresses(id),
    user_coupon_id  BIGINT      REFERENCES user_coupons(id),
    total_price     INT         NOT NULL,
    discount_price  INT         NOT NULL DEFAULT 0,
    point_used      INT         NOT NULL DEFAULT 0,
    final_price     INT         NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id                BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id          BIGINT NOT NULL REFERENCES orders(id),
    product_option_id BIGINT NOT NULL REFERENCES product_options(id),
    quantity          INT    NOT NULL,
    price             INT    NOT NULL
);

CREATE TABLE payments (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT       NOT NULL UNIQUE REFERENCES orders(id),
    method      VARCHAR(50)  NOT NULL,
    amount      INT          NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'READY',
    pg_tx_id    VARCHAR(255),
    paid_at     DATETIME,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE shipments (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT       NOT NULL UNIQUE REFERENCES orders(id),
    carrier         VARCHAR(100) NOT NULL,
    tracking_number VARCHAR(100) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PREPARING',
    shipped_at      DATETIME,
    delivered_at    DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE point_histories (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id),
    amount     INT          NOT NULL,
    reason     VARCHAR(255) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reviews (
    id            BIGINT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT   NOT NULL REFERENCES users(id),
    product_id    BIGINT   NOT NULL REFERENCES products(id),
    order_item_id BIGINT   NOT NULL UNIQUE REFERENCES order_items(id),
    rating        TINYINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    content       TEXT,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE review_images (
    id        BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT       NOT NULL REFERENCES reviews(id),
    url       VARCHAR(500) NOT NULL
);

CREATE TABLE notifications (
    id         BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id),
    type       VARCHAR(50) NOT NULL,
    message    TEXT        NOT NULL,
    is_read    TINYINT(1)  NOT NULL DEFAULT 0,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
