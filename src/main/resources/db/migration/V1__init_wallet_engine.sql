-- ============================================================================
-- 1. BASE LAYER EXTENSIONS & ENUMS
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; 

CREATE TYPE compliance_tier AS ENUM ('TIER_1', 'TIER_2', 'TIER_3');
CREATE TYPE status  AS ENUM ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED');
CREATE TYPE saga_status      AS ENUM ('STARTED', 'LEDGER_HOLD_SUCCESS', 'VENDOR_CALL_SUCCESS', 'COMPLETED', 'FAILED', 'COMPENSATING');
CREATE TYPE service_type     AS ENUM ('FUND', 'TRANSFER', 'WITHDRAW', 'AIRTIME', 'DATA_SUBSCRIPTION', 'UTILITY_BILL', 'CABLE_TV');
CREATE TYPE service_status   AS ENUM ('INITIATED', 'PENDING_PROVIDER', 'SUCCESS', 'FAILED');

CREATE TYPE account_type AS ENUM ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE');


-- ============================================================================
-- 2. CLIENT IDENTITY BOUNDS & COMPLIANCE LAYER
-- ============================================================================
CREATE TABLE compliance_limits (
    tier compliance_tier PRIMARY KEY,
    single_transaction_limit BIGINT NOT NULL CHECK (single_transaction_limit > 0),
    daily_velocity_limit BIGINT NOT NULL CHECK (daily_velocity_limit > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO compliance_limits (tier, single_transaction_limit, daily_velocity_limit) VALUES 
('TIER_1', 10000, 50000),   
('TIER_2', 250000, 1000000), 
('TIER_3', 9223372036854775807, 9223372036854775807); 

CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type account_type NOT NULL,
    currency VARCHAR(3) NOT NULL, -- ISO 4217 Currency Code (e.g., 'GBP')
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(64) UNIQUE NOT NULL,
    password VARCHAR(64) NOT NULL,
    status status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    role VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(32) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================================
-- 3. CORE LAYER
-- ============================================================================
CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    ledger_account_id VARCHAR(64), 
    available BIGINT NOT NULL DEFAULT 0 CHECK (available >= 0),
    held      BIGINT NOT NULL DEFAULT 0 CHECK (held >= 0),
    currency VARCHAR(3) NOT NULL, 
    --status account_status NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 0, -- Optimistic locking
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE wallet_transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    service_type service_type NOT NULL, -- FUND, P2P, VAS, PAYOUT
    amount BIGINT NOT NULL, -- Amounts are integer pence
    fee BIGINT NOT NULL DEFAULT 0,
    counterparty_wallet BIGINT,
    service_status VARCHAR(50) NOT NULL, -- PENDING, SUCCESS, FAILED
    --reference_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE banks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) UNIQUE NOT NULL, 
    code VARCHAR(255) UNIQUE NOT NULL, 
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL, 
    number VARCHAR(255) NOT NULL,
    bvn VARCHAR(255) UNIQUE NOT NULL,
    bank_id UUID NOT NULL REFERENCES banks(id),
    user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE service_providers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) UNIQUE NOT NULL,
    type VARCHAR(64) NOT NULL, -- percent or amount
    cost DECIMAL NOT NULL,
    revenue DECIMAL NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO service_providers (name, type, cost, revenue) VALUES ('FUNDING', 'percent', 0.01, 0.005);

CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name service_type NOT NULL, -- FIXED: Uses the enum type correctly
    service_providers_id UUID NOT NULL REFERENCES service_providers(id), -- FIXED: Added data type UUID and correctly ordered keywords
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS inbox (
   user_id UUID NOT NULL REFERENCES users(id),
    message_id VARCHAR(255) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, message_id)
);


CREATE TABLE IF NOT EXISTS outbox (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    target VARCHAR(64) NOT NULL,
    path VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    body JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending
    ON outbox (next_attempt_at, id)
    WHERE status = 'PENDING';
