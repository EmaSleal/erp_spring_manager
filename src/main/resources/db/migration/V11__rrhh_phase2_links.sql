-- V11: RRHH Phase 2 links
-- Adds unique constraint on empleados.usuario_id and jefe_id FK on departamentos.
-- Note: ADD UNIQUE will fail naturally if duplicate usuario_id values exist in the data.

ALTER TABLE empleados ADD UNIQUE KEY uq_empleado_usuario (usuario_id);

ALTER TABLE departamentos ADD COLUMN jefe_id BIGINT NULL;

ALTER TABLE departamentos ADD CONSTRAINT fk_dept_jefe FOREIGN KEY (jefe_id) REFERENCES empleados(id);
