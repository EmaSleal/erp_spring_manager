-- T-039 / ADR-045 §1: factura sustituta. Vincula una factura nueva con la factura anulada que reemplaza.
-- FK nullable sin cascada; la columna generada deja libre el cupo cuando la sustituta se anula, de modo que
-- cada original admite una sola sustituta no anulada (varias filas con NULL son válidas en un UNIQUE de MySQL).
ALTER TABLE factura
    ADD COLUMN sustituye_factura_id INT NULL,
    ADD COLUMN razon_sustitucion VARCHAR(180) NULL,
    ADD COLUMN codigo_sustitucion VARCHAR(2) NULL,
    ADD COLUMN sustituta_vigente_de_id INT GENERATED ALWAYS AS (IF(anulada = 0, sustituye_factura_id, NULL)) STORED,
    ADD KEY idx_factura_sustituye (sustituye_factura_id),
    ADD UNIQUE KEY uk_factura_una_sustituta_vigente (sustituta_vigente_de_id),
    ADD CONSTRAINT fk_factura_sustituye FOREIGN KEY (sustituye_factura_id) REFERENCES factura (id_factura);
