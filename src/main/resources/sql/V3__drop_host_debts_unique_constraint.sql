-- Script para eliminar la restricción única de (venue_id, cycle_start, cycle_end) en la tabla host_debts
-- Esto permite que cada reserva (booking_id) genere su propia fila independiente en la tabla host_debts.

ALTER TABLE host_debts DROP CONSTRAINT IF EXISTS idx_host_debts_venue_cycle;
DROP INDEX IF EXISTS idx_host_debts_venue_cycle;
