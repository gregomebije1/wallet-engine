-- ============================================================================
-- 1. BASE LAYER EXTENSIONS & ENUMS
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; 

CREATE TYPE compliance_tier AS ENUM ('TIER_1', 'TIER_2', 'TIER_3');
CREATE TYPE account_status  AS ENUM ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED');
CREATE TYPE saga_status      AS ENUM ('STARTED', 'LEDGER_HOLD_SUCCESS', 'VENDOR_CALL_SUCCESS', 'COMPLETED', 'FAILED', 'COMPENSATING');
CREATE TYPE service_type     AS ENUM ('FUND', 'TRANSFER', 'WITHDRAW', 'AIRTIME', 'DATA_SUBSCRIPTION', 'UTILITY_BILL', 'CABLE_TV');
CREATE TYPE funding_status   AS ENUM ('INITIATED', 'PENDING_PROVIDER', 'SUCCESS', 'FAILED');
CREATE TYPE service_status   AS ENUM ('INITIATED', 'PENDING_PROVIDER', 'SUCCESS', 'FAILED');

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

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    --user_reference VARCHAR(64) UNIQUE NOT NULL, 
    --compliance_tier compliance_tier NOT NULL DEFAULT 'TIER_1',
    status account_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    --password VARCHAR(64) NOT NULL,
    --firstname VARCHAR(64) NOT NULL,
    --lastname VARCHAR(64) NOT NULL,
    --phone_number VARCHAR(64) UNIQUE NOT NULL,
    email VARCHAR(64) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================================
-- 3. CORE LAYER
-- ============================================================================
CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    ledger_account_id VARCHAR(64) UNIQUE NOT NULL, 
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
    type service_type NOT NULL, -- FUND, P2P, VAS, PAYOUT
    amount BIGINT NOT NULL, -- Amounts are integer pence
    fee BIGINT NOT NULL DEFAULT 0,
    counterparty_wallet BIGINT,
    status VARCHAR(50) NOT NULL, -- PENDING, SUCCESS, FAILED
    --reference_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), 
  topic TEXT NOT NULL, 
  payload JSONB NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, 
  published_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE inbox (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), 
    event_id TEXT NOT NULL, 
    received_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
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
    cost BIGINT NOT NULL,
    revenue BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name service_type NOT NULL, -- FIXED: Uses the enum type correctly
    service_providers_id UUID NOT NULL REFERENCES service_providers(id), -- FIXED: Added data type UUID and correctly ordered keywords
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);


