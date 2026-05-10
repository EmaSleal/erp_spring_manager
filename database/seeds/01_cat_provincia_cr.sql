-- ============================================================
-- Seed: cat_provincia_cr
-- FK deps: none
-- Idempotency: ON DUPLICATE KEY UPDATE
-- ============================================================
SET NAMES utf8mb4;
USE facturas_monrachem;

INSERT INTO cat_provincia_cr (codigo, nombre) VALUES
('1', 'San José'),
('2', 'Alajuela'),
('3', 'Cartago'),
('4', 'Heredia'),
('5', 'Guanacaste'),
('6', 'Puntarenas'),
('7', 'Limón')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);
