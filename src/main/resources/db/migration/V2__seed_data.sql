-- Demo data. Test PANs only. pan_hash = SHA-256(PAN).
-- Amounts are in minor units (cents). window_date is set far in the past so the
-- daily counter rolls over to "today" on the first authorization.

-- Account 1: healthy balance ($500.00) -> approvals
INSERT INTO account (id, balance, currency, status) VALUES (1, 50000, '840', 'ACTIVE');
-- Account 2: tiny balance ($5.00)       -> insufficient funds (51)
INSERT INTO account (id, balance, currency, status) VALUES (2, 500, '840', 'ACTIVE');
-- Account 3: backs the blocked card
INSERT INTO account (id, balance, currency, status) VALUES (3, 50000, '840', 'ACTIVE');
-- Account 4: backs the expired card
INSERT INTO account (id, balance, currency, status) VALUES (4, 50000, '840', 'ACTIVE');
-- Account 5: backs the issuer-unavailable card
INSERT INTO account (id, balance, currency, status) VALUES (5, 50000, '840', 'ACTIVE');

SELECT setval('account_id_seq', 5, true);

-- PAN 4111111111111111 : ACTIVE, approves
INSERT INTO card (id, pan_hash, pan_last4, expiry, status, account_id)
VALUES (1, '9bbef19476623ca56c17da75fd57734dbf82530686043a6e491c6d71befe8f6e', '1111', '3012', 'ACTIVE', 1);
-- PAN 5555555555554444 : ACTIVE, low balance -> 51
INSERT INTO card (id, pan_hash, pan_last4, expiry, status, account_id)
VALUES (2, '2f725bbd1f405a1ed0336abaf85ddfeb6902a9984a76fd877c3b5cc3b5085a82', '4444', '3012', 'ACTIVE', 2);
-- PAN 4000000000000002 : BLOCKED -> 05
INSERT INTO card (id, pan_hash, pan_last4, expiry, status, account_id)
VALUES (3, 'acd08f29a41f2e55ab0c4f774b1562b03ff01a905ed5b100f4facd43af572b1b', '0002', '3012', 'BLOCKED', 3);
-- PAN 4000000000000010 : expired (YYMM 2001 = Jan 2020) -> 54
INSERT INTO card (id, pan_hash, pan_last4, expiry, status, account_id)
VALUES (4, '3425f1b24ad8cef732fa8142535dc537ab9ad3db3bda3b8f9d0ea82b9b749044', '0010', '2001', 'ACTIVE', 4);
-- PAN 9999000000000004 : issuer unavailable (BIN 9999) -> 91
INSERT INTO card (id, pan_hash, pan_last4, expiry, status, account_id)
VALUES (5, '6d9ad455cd9640117fcef8b921e89ddcdb283e47400ba65854a7804cd73c1922', '0004', '3012', 'ACTIVE', 5);

SELECT setval('card_id_seq', 5, true);

-- Limits: per-txn $200.00, daily $300.00 for all demo cards
INSERT INTO card_limit (id, card_id, per_txn_limit, daily_limit, daily_spent, window_date) VALUES (1, 1, 20000, 30000, 0, DATE '2000-01-01');
INSERT INTO card_limit (id, card_id, per_txn_limit, daily_limit, daily_spent, window_date) VALUES (2, 2, 20000, 30000, 0, DATE '2000-01-01');
INSERT INTO card_limit (id, card_id, per_txn_limit, daily_limit, daily_spent, window_date) VALUES (3, 3, 20000, 30000, 0, DATE '2000-01-01');
INSERT INTO card_limit (id, card_id, per_txn_limit, daily_limit, daily_spent, window_date) VALUES (4, 4, 20000, 30000, 0, DATE '2000-01-01');
INSERT INTO card_limit (id, card_id, per_txn_limit, daily_limit, daily_spent, window_date) VALUES (5, 5, 20000, 30000, 0, DATE '2000-01-01');

SELECT setval('card_limit_id_seq', 5, true);
