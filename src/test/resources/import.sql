-- T-059: en los ITs el esquema lo crea Hibernate (sin Flyway), por lo que la columna NOT NULL pdf_rts_titulo no trae el
-- DEFAULT de V64. Este script (Hibernate lo corre tras create/create-drop) lo reproduce. El texto vive solo en pruebas.
ALTER TABLE configuracion_facturacion ALTER COLUMN pdf_rts_titulo SET DEFAULT 'Factura — Régimen de Tributación Simplificada';
