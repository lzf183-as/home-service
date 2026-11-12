-- ============================================================
-- 社区家政服务预约网站 数据库脚本
-- 语法兼容 H2 (MODE=MySQL) 与 MySQL 8.0
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    real_name   VARCHAR(50),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    role        VARCHAR(20)  NOT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL,
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS staff (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    job_type    VARCHAR(30)  NOT NULL,
    staff_level VARCHAR(20)  NOT NULL,
    rating      DECIMAL(2,1) NOT NULL DEFAULT 5.0,
    order_count INT          NOT NULL DEFAULT 0,
    available   TINYINT      NOT NULL DEFAULT 1,
    intro       VARCHAR(500),
    created_at  DATETIME     NOT NULL
);

CREATE TABLE IF NOT EXISTS service_category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    icon        VARCHAR(20),
    description VARCHAR(200),
    sort_no     INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    created_at  DATETIME     NOT NULL
);

CREATE TABLE IF NOT EXISTS service_item (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT        NOT NULL,
    name        VARCHAR(80)   NOT NULL,
    subtitle    VARCHAR(150),
    price       DECIMAL(10,2) NOT NULL,
    price_unit  VARCHAR(10)   NOT NULL DEFAULT '次',
    duration    INT           NOT NULL DEFAULT 120,
    cover       VARCHAR(20),
    description TEXT,
    sales       INT           NOT NULL DEFAULT 0,
    rating      DECIMAL(2,1)  NOT NULL DEFAULT 5.0,
    status      TINYINT       NOT NULL DEFAULT 1,
    created_at  DATETIME      NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_address (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    contact_name  VARCHAR(50)  NOT NULL,
    contact_phone VARCHAR(20)  NOT NULL,
    region        VARCHAR(100) NOT NULL,
    detail        VARCHAR(200) NOT NULL,
    is_default    TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL
);

CREATE TABLE IF NOT EXISTS service_order (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no         VARCHAR(32)   NOT NULL,
    user_id          BIGINT        NOT NULL,
    service_id       BIGINT        NOT NULL,
    staff_id         BIGINT,
    service_name     VARCHAR(80)   NOT NULL,
    service_price    DECIMAL(10,2) NOT NULL,
    quantity         INT           NOT NULL DEFAULT 1,
    amount           DECIMAL(10,2) NOT NULL,
    appointment_date DATE          NOT NULL,
    appointment_time VARCHAR(20)   NOT NULL,
    contact_name     VARCHAR(50)   NOT NULL,
    contact_phone    VARCHAR(20)   NOT NULL,
    address          VARCHAR(300)  NOT NULL,
    remark           VARCHAR(500),
    status           VARCHAR(20)   NOT NULL,
    cancel_reason    VARCHAR(200),
    created_at       DATETIME      NOT NULL,
    accepted_at      DATETIME,
    started_at       DATETIME,
    finished_at      DATETIME,
    cancelled_at     DATETIME,
    CONSTRAINT uk_order_no UNIQUE (order_no)
);

CREATE TABLE IF NOT EXISTS order_review (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    staff_id    BIGINT,
    service_id  BIGINT       NOT NULL,
    rating      INT          NOT NULL,
    content     VARCHAR(500),
    created_at  DATETIME     NOT NULL,
    CONSTRAINT uk_review_order UNIQUE (order_id)
);

CREATE TABLE IF NOT EXISTS order_status_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT       NOT NULL,
    from_status VARCHAR(20),
    to_status   VARCHAR(20)  NOT NULL,
    operator    VARCHAR(50),
    remark      VARCHAR(200),
    created_at  DATETIME     NOT NULL
);
