-- ============================================================================
-- V28: CxP manual con desglose y contrapartida (ADR-015, ADR-016, T-012)
-- ============================================================================
-- New columns on cuentas_por_pagar (all NULL-able):
--   subtotal        DECIMAL(15,2) — amount before VAT of a manual CxP
--   impuesto        DECIMAL(15,2) — VAT (crédito fiscal) of a manual CxP
--   cuenta_gasto_id BIGINT        — counterpart account chosen by the user (FK cuentas_contables)
-- Existing rows and CxP created from a purchase order keep NULL: the breakdown of
-- an OC-backed CxP comes from the OC. Requiredness for a manual CxP is enforced by
-- CuentaPorPagarServiceImpl.crear(). No backfill: existing manual CxP do not get
-- a purchase entry (out of scope of T-012).
-- The table has no CREATE TABLE in Flyway (created historically by Hibernate),
-- so only ALTER TABLE is used.
-- ============================================================================

ALTER TABLE cuentas_por_pagar
    ADD COLUMN subtotal        DECIMAL(15,2) NULL,
    ADD COLUMN impuesto        DECIMAL(15,2) NULL,
    ADD COLUMN cuenta_gasto_id BIGINT        NULL;

ALTER TABLE cuentas_por_pagar
    ADD CONSTRAINT fk_cpp_cuenta_gasto
        FOREIGN KEY (cuenta_gasto_id) REFERENCES cuentas_contables (id);
