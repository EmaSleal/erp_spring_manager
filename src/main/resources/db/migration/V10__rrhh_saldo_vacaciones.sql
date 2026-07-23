-- V10: Vacation balance tracking — Código de Trabajo Art. 153
-- Employees earn minimum 2 weeks (10 business days) per 50 weeks of continuous work.

CREATE TABLE saldo_vacaciones (
  id BIGINT NOT NULL AUTO_INCREMENT,
  empleado_id BIGINT NOT NULL,
  dias_generados DECIMAL(6,2) NOT NULL DEFAULT 0,
  dias_disfrutados DECIMAL(6,2) NOT NULL DEFAULT 0,
  dias_disponibles DECIMAL(6,2) GENERATED ALWAYS AS (dias_generados - dias_disfrutados) STORED,
  fecha_ultimo_calculo DATE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_saldo_vac_empleado (empleado_id),
  CONSTRAINT fk_saldo_vac_empleado FOREIGN KEY (empleado_id) REFERENCES empleados(id)
);
