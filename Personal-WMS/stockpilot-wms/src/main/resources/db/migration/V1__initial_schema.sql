-- StockPilot WMS - initial schema
-- Flyway runs this once. Never edit it after it has run; add a new V<n>__ file instead.

CREATE TABLE uom (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(10)    NOT NULL UNIQUE,
    name            VARCHAR(50)    NOT NULL,
    -- How many eaches (EA) one of this unit contains. EA itself is 1.
    factor_to_each  DECIMAL(19,4)  NOT NULL
);

CREATE TABLE location (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    code       VARCHAR(30)  NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    zone       VARCHAR(30)  NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE part (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_number     VARCHAR(40)    NOT NULL UNIQUE,
    description     VARCHAR(255)   NOT NULL,
    standard_cost   DECIMAL(19,4)  NOT NULL DEFAULT 0,
    reorder_point   DECIMAL(19,4)  NOT NULL DEFAULT 0,
    -- Quantity promised to open sales orders (in EA). Kept in sync by the sales order service.
    qty_allocated   DECIMAL(19,4)  NOT NULL DEFAULT 0,
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)    NOT NULL
);

CREATE TABLE vendor (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    code    VARCHAR(20)  NOT NULL UNIQUE,
    name    VARCHAR(100) NOT NULL,
    email   VARCHAR(100),
    phone   VARCHAR(30)
);

CREATE TABLE customer (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(100),
    ship_to     VARCHAR(255)
);

CREATE TABLE inventory_level (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_id      BIGINT        NOT NULL,
    location_id  BIGINT        NOT NULL,
    qty_on_hand  DECIMAL(19,4) NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_part_location UNIQUE (part_id, location_id),
    CONSTRAINT fk_inventory_part FOREIGN KEY (part_id) REFERENCES part (id),
    CONSTRAINT fk_inventory_location FOREIGN KEY (location_id) REFERENCES location (id)
);

CREATE TABLE inventory_transaction (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_id      BIGINT        NOT NULL,
    location_id  BIGINT        NOT NULL,
    qty_change   DECIMAL(19,4) NOT NULL,
    type         VARCHAR(20)   NOT NULL,
    reference    VARCHAR(100),
    note         VARCHAR(255),
    username     VARCHAR(50)   NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    CONSTRAINT fk_txn_part FOREIGN KEY (part_id) REFERENCES part (id),
    CONSTRAINT fk_txn_location FOREIGN KEY (location_id) REFERENCES location (id)
);

CREATE TABLE purchase_order (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_number      VARCHAR(20)  UNIQUE,
    vendor_id      BIGINT       NOT NULL,
    status         VARCHAR(30)  NOT NULL,
    order_date     DATE         NOT NULL,
    expected_date  DATE,
    notes          VARCHAR(255),
    created_at     DATETIME(6)  NOT NULL,
    CONSTRAINT fk_po_vendor FOREIGN KEY (vendor_id) REFERENCES vendor (id)
);

CREATE TABLE purchase_order_line (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id  BIGINT        NOT NULL,
    line_no            INT           NOT NULL,
    part_id            BIGINT        NOT NULL,
    uom_id             BIGINT        NOT NULL,
    qty_ordered        DECIMAL(19,4) NOT NULL,
    qty_received       DECIMAL(19,4) NOT NULL DEFAULT 0,
    unit_cost          DECIMAL(19,4) NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    CONSTRAINT fk_pol_po FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (id),
    CONSTRAINT fk_pol_part FOREIGN KEY (part_id) REFERENCES part (id),
    CONSTRAINT fk_pol_uom FOREIGN KEY (uom_id) REFERENCES uom (id)
);

CREATE TABLE sales_order (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    so_number        VARCHAR(20)  UNIQUE,
    customer_id      BIGINT       NOT NULL,
    status           VARCHAR(30)  NOT NULL,
    order_date       DATE         NOT NULL,
    external_ref     VARCHAR(60),
    source           VARCHAR(20)  NOT NULL,
    carrier          VARCHAR(40),
    tracking_number  VARCHAR(60),
    shipped_at       DATETIME(6),
    created_at       DATETIME(6)  NOT NULL,
    CONSTRAINT fk_so_customer FOREIGN KEY (customer_id) REFERENCES customer (id)
);

CREATE TABLE sales_order_line (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sales_order_id  BIGINT        NOT NULL,
    line_no         INT           NOT NULL,
    part_id         BIGINT        NOT NULL,
    qty_ordered     DECIMAL(19,4) NOT NULL,
    qty_picked      DECIMAL(19,4) NOT NULL DEFAULT 0,
    unit_price      DECIMAL(19,4) NOT NULL,
    CONSTRAINT fk_sol_so FOREIGN KEY (sales_order_id) REFERENCES sales_order (id),
    CONSTRAINT fk_sol_part FOREIGN KEY (part_id) REFERENCES part (id)
);

CREATE TABLE app_user (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE
);
