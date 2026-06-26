-- Core schema for the authorization switch.

CREATE TABLE account (
    id       BIGSERIAL PRIMARY KEY,
    balance  BIGINT      NOT NULL,           -- minor units (cents)
    currency VARCHAR(3)  NOT NULL,           -- ISO 4217 numeric, e.g. 840
    status   VARCHAR(16) NOT NULL,
    version  BIGINT      NOT NULL DEFAULT 0  -- optimistic lock
);

CREATE TABLE card (
    id         BIGSERIAL PRIMARY KEY,
    pan_hash   VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 of PAN; raw PAN never stored
    pan_last4  VARCHAR(4)  NOT NULL,
    expiry     VARCHAR(4)  NOT NULL,         -- YYMM
    status     VARCHAR(16) NOT NULL,
    account_id BIGINT      NOT NULL REFERENCES account(id)
);

CREATE TABLE card_limit (
    id            BIGSERIAL PRIMARY KEY,
    card_id       BIGINT  NOT NULL UNIQUE REFERENCES card(id),
    per_txn_limit BIGINT  NOT NULL,
    daily_limit   BIGINT  NOT NULL,
    daily_spent   BIGINT  NOT NULL DEFAULT 0,
    window_date   DATE    NOT NULL
);

CREATE TABLE transaction (
    id            BIGSERIAL PRIMARY KEY,
    stan          VARCHAR(12) NOT NULL,
    rrn           VARCHAR(12) NOT NULL,
    pan_last4     VARCHAR(4)  NOT NULL,
    mti           VARCHAR(4)  NOT NULL,
    amount        BIGINT      NOT NULL,
    currency      VARCHAR(3)  NOT NULL,
    response_code VARCHAR(2)  NOT NULL,
    approved      BOOLEAN     NOT NULL,
    auth_code     VARCHAR(6),
    latency_ms    BIGINT      NOT NULL,
    created_at    TIMESTAMP   NOT NULL,
    CONSTRAINT uq_txn_stan_rrn UNIQUE (stan, rrn)
);

CREATE INDEX idx_txn_created_at ON transaction (created_at);
