-- ============================================================
-- DATOS INICIALES: PROVINCIAS DE COSTA RICA
-- ============================================================
-- Fuente: Ministerio de Hacienda - División Territorial Administrativa
-- Generado automáticamente

INSERT INTO cat_provincia_cr (codigo, nombre) VALUES
('1', 'San José'),
('2', 'Alajuela'),
('3', 'Cartago'),
('4', 'Heredia'),
('5', 'Guanacaste'),
('6', 'Puntarenas'),
('7', 'Limón')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);
