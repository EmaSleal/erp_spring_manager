-- V9: CR compliance gap fields for contratos_empleado and ausencias
-- Código de Trabajo compliance: lugar de trabajo, periodo de prueba,
-- jornada parcial, forma y periodicidad de pago; certificación de incapacidad.

ALTER TABLE contratos_empleado
  ADD COLUMN lugar_trabajo VARCHAR(200) NULL,
  ADD COLUMN fecha_fin_periodo_prueba DATE NULL,
  ADD COLUMN horas_semanales INT NULL,
  ADD COLUMN forma_pago VARCHAR(50) NULL,
  ADD COLUMN periodicidad_pago VARCHAR(50) NULL;

ALTER TABLE ausencias
  ADD COLUMN entidad_certificante VARCHAR(100) NULL,
  ADD COLUMN numero_boleta VARCHAR(50) NULL,
  ADD COLUMN porcentaje_patrono DECIMAL(5,4) NULL,
  ADD COLUMN porcentaje_subsidio DECIMAL(5,4) NULL;
