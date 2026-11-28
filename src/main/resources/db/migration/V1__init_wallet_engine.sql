-- ============================================================================
-- 1. BASE LAYER EXTENSIONS & ENUMS
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto"; 

CREATE TYPE compliance_tier AS ENUM ('TIER_1', 'TIER_2', 'TIER_3');
CREATE TYPE status  AS ENUM ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED');
CREATE TYPE saga_status      AS ENUM ('STARTED', 'LEDGER_HOLD_SUCCESS', 'VENDOR_CALL_SUCCESS', 'COMPLETED', 'FAILED', 'COMPENSATING');
CREATE TYPE service_type     AS ENUM ('FUND', 'TRANSFER', 'WITHDRAW', 'AIRTIME', 'DATA_SUBSCRIPTION', 'UTILITY_BILL', 'CABLE_TV');
CREATE TYPE service_status   AS ENUM ('INITIATED', 'PENDING', 'SUCCESS', 'FAILED');

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

--CREATE TABLE roles (
--    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
--    name VARCHAR(32) NOT NULL UNIQUE
--);

--CREATE TABLE user_roles (
--    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
--    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
--    PRIMARY KEY (user_id, role_id)
--);

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
    idempotency_key UUID UNIQUE NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    service_type service_type NOT NULL, -- FUND, P2P, VAS, PAYOUT
    amount BIGINT NOT NULL, -- Amounts are integer pence
    fee BIGINT NOT NULL DEFAULT 0,
    counterparty_wallet BIGINT,
    service_status service_status NOT NULL, -- PENDING, SUCCESS, FAILED
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




CREATE TABLE outbox (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  -- user_id UUID NOT NULL REFERENCES users(id),
  target TEXT NOT NULL, 
  path TEXT NOT NULL,
  idempotency_key UUID NOT NULL, 
  body JSONB NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  attempts INT DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  published_at TIMESTAMPTZ,
  next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now());


CREATE INDEX IF NOT EXISTS idx_outbox_pending
    ON outbox (next_attempt_at, id)
    WHERE status = 'PENDING';

CREATE TABLE IF NOT EXISTS inbox (
    message_id  UUID NOT NULL UNIQUE PRIMARY KEY,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE FUNCTION eline(account TEXT, sub_ref UUID, debit BIGINT, credit BIGINT) RETURNS JSONB
LANGUAGE sql IMMUTABLE AS $$
  SELECT jsonb_build_object('account', account, 'sub_ref', sub_ref, 'debit', debit, 'credit', credit)  $$;

CREATE FUNCTION enqueue_entry(entry_id UUID, book TEXT, memo TEXT, lines JSONB[]) RETURNS void
LANGUAGE sql AS $$
  INSERT INTO outbox (target, path, idempotency_key, body)
  VALUES ('ledger', '/ledger/v1/entries', entry_id,
          jsonb_build_object('entry_id', entry_id, 'book', book, 'memo', memo, 'lines', to_jsonb(lines))) $$;


SELECT proname, pg_get_function_arguments(oid) FROM pg_proc WHERE proname = 'fn_capture_deposit';


DROP FUNCTION IF EXISTS fn_create_deposit(UUID, UUID, BIGINT, BIGINT);
CREATE OR REPLACE FUNCTION fn_create_deposit(
    -- Implicit transaction inside PL/pgSQL function
    p_idemp_key UUID,
    p_wallet_id UUID,
    p_amount BIGINT,
    p_fee BIGINT
)
RETURNS TABLE (
    idempotency_key UUID,
    wallet_id UUID,
    service_type service_type,
    amount BIGINT,
    fee BIGINT,
    service_status service_status,
    created_at TIMESTAMPTZ
)
LANGUAGE plpgsql
AS $$
#variable_conflict use_column
BEGIN
    -- 1. Atomic insert into wallet_transactions + outbox in a single statement
    WITH t AS (
        INSERT INTO wallet_transactions (wallet_id, idempotency_key, service_type, amount, fee, service_status)
        VALUES (p_wallet_id, p_idemp_key, 'FUND', p_amount, p_fee, 'PENDING')
        ON CONFLICT (idempotency_key) DO NOTHING
        RETURNING wallet_transactions.idempotency_key, wallet_transactions.amount, wallet_transactions.fee
    )
    INSERT INTO outbox (target, path, idempotency_key, body)
    SELECT 'gateway', '/gateway/v1/charges', t.idempotency_key,
           jsonb_build_object('charge_id', t.idempotency_key, 'amount', t.amount + t.fee)
    FROM t;

    -- 2. Return either the newly created row or the previously existing row
    RETURN QUERY
    SELECT w.idempotency_key, w.wallet_id, w.service_type, w.amount, w.fee, w.service_status, w.created_at
    FROM wallet_transactions w
    WHERE w.idempotency_key = p_idemp_key;
END;
$$;


--SELECT * FROM fn_create_deposit(
--    'f0000000-0000-0000-0000-000000000001'::UUID,
--    '65d9f3ef-3a42-405b-ae97-405b5b9ab272'::UUID,
--    10000,
--    300);

-----------------------------
CREATE OR REPLACE FUNCTION fn_capture_deposit(
    p_msg_id UUID,       -- From Idempotency-Key header (e.g. e0000000-...-0001)
    p_charge_id UUID     -- From body.charge_id (e.g. d0000000-...-0001) -> Changed from VARCHAR to UUID
)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    v_wallet_id UUID;
    v_amount BIGINT;
    v_fee BIGINT;
BEGIN
    -- 1. Inbox idempotency guard (tracks webhook message delivery)
    INSERT INTO inbox (message_id)
    VALUES (p_msg_id)
    ON CONFLICT (message_id) DO NOTHING;

    IF NOT FOUND THEN
        RETURN 'DUPLICATE';
    END IF;

    -- 2. Transition deposit to SUCCESS using p_charge_id (not p_msg_id!)
    UPDATE wallet_transactions
    SET service_status = 'SUCCESS'
    WHERE idempotency_key = p_charge_id
      AND service_status = 'PENDING'
    RETURNING wallet_id, amount, fee
    INTO v_wallet_id, v_amount, v_fee;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Deposit % not found or not PENDING', p_charge_id;
    END IF;

    -- 3. Credit wallet available balance
    UPDATE wallets
    SET available = available + v_amount
    WHERE id = v_wallet_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Wallet % not found', v_wallet_id;
    END IF;

    -- 4. Enqueue double-entry ledger lines (p_charge_id is now UUID, matching enqueue_entry)
    PERFORM enqueue_entry(
        p_charge_id,
        'wallet',
        'Deposit captured',
        ARRAY[
            eline('W300', NULL, v_amount + v_fee, 0),
            eline('W100', v_wallet_id, 0, v_amount),
            eline('W200', NULL, 0, v_fee)
        ]
    );

    RETURN 'CAPTURED';
END;
$$;

-------
--DROP FUNCTION IF EXISTS fn_capture_deposit(UUID, VARCHAR);

--SELECT fn_capture_deposit(
--    'f0000000-0000-0000-0000-000000000002'::UUID, -- p_msg_id (Idempotency-Key header)
--    'f0000000-0000-0000-0000-000000000001'::UUID  -- p_charge_id (body.charge_id)
--);

-------
CREATE OR REPLACE FUNCTION fn_settle_gateway(
    p_msg_id UUID,       -- From Idempotency-Key header (:msg2) -> also used as entry_id
    p_charge_id UUID,    -- From body.charge_id ("d0000000-...-0001")
    p_gross BIGINT,      -- From body.gross (10300)
    p_fee BIGINT,        -- From body.fee (150)
    p_net BIGINT         -- From body.net (10150)
)
RETURNS TEXT
LANGUAGE plpgsql
AS $$
BEGIN
    -- 1. Inbox idempotency guard
    INSERT INTO inbox (message_id)
    VALUES (p_msg_id)
    ON CONFLICT (message_id) DO NOTHING;

    IF NOT FOUND THEN
        RETURN 'DUPLICATE';
    END IF;

    -- 2. Ensure double-entry debits equal credits (10150 + 150 = 10300)
    IF p_gross <> (p_net + p_fee) THEN
        RAISE EXCEPTION 'Unbalanced settlement for charge %: gross (%) != net (%) + fee (%)',
            p_charge_id, p_gross, p_net, p_fee;
    END IF;

    -- 3. Enqueue double-entry ledger lines using p_msg_id as entry_id
    PERFORM enqueue_entry(
        p_msg_id,
        'wallet',
        'Gateway settlement',
        ARRAY[
            eline('W010', NULL, p_net, 0),
            eline('W210', NULL, p_fee, 0),
            eline('W300', NULL, 0, p_gross)
        ]
    );

    RETURN 'SETTLED';
END;
$$;

-- SELECT fn_settle_gateway(
--    'e0000000-0000-0000-0000-000000000002'::UUID, -- p_msg_id (Idempotency-Key header & entry_id)
--    'd0000000-0000-0000-0000-000000000001'::UUID, -- p_charge_id
--    10300,                                        -- p_gross
--    150,                                          -- p_fee
--    10150                                         -- p_net
--);
