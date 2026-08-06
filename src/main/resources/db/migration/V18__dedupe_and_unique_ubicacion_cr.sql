-- Removes duplicate rows produced by re-running database/seeds/02_cat_canton_cr.sql
-- and 03_cat_distrito_cr.sql on every deploy without a natural-key unique constraint
-- to back the ON DUPLICATE KEY UPDATE clause. Keeps the lowest id per natural key.

DELETE c1 FROM cat_canton_cr c1
JOIN cat_canton_cr c2
  ON c1.provincia_codigo = c2.provincia_codigo
 AND c1.codigo = c2.codigo
 AND c1.id > c2.id;

ALTER TABLE cat_canton_cr
  ADD CONSTRAINT uq_canton_provincia_codigo UNIQUE (provincia_codigo, codigo);

DELETE d1 FROM cat_distrito_cr d1
JOIN cat_distrito_cr d2
  ON d1.provincia_codigo = d2.provincia_codigo
 AND d1.canton_codigo = d2.canton_codigo
 AND d1.codigo = d2.codigo
 AND d1.id > d2.id;

ALTER TABLE cat_distrito_cr
  ADD CONSTRAINT uq_distrito_provincia_canton_codigo UNIQUE (provincia_codigo, canton_codigo, codigo);
