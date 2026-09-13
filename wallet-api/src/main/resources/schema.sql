-- Enable pgcrypto for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Use text/varchar IDs because application prepends prefixes (e.g. WALLET_... / TRANSFER_...)
CREATE TABLE IF NOT EXISTS wallets (
    wallet_id VARCHAR(255) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    balance_paise BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transfers (
    transfer_id VARCHAR(255) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    from_wallet_id VARCHAR(255) NOT NULL REFERENCES wallets(wallet_id) ON DELETE CASCADE,
    to_wallet_id VARCHAR(255) NOT NULL REFERENCES wallets(wallet_id) ON DELETE CASCADE,
    amount_paise BIGINT NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    declined_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (idempotency_key)
);
