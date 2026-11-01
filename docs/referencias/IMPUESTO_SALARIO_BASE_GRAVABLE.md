# Impuesto al salario: base gravable (P-056)

Estado: **investigación preliminar, pendiente de validación del contador**. No es asesoría legal ni tributaria.
Fecha: 2026-11-01.

## 1. Problema

`CalculosNominaService.calcular` calcula la renta con:

```
baseGravable = brutoProrrateado − ccssObrero
```

(Hasta T-027 también restaba el INS; ya no).

Hipótesis: el impuesto único sobre rentas del trabajo dependiente (Título II, Ley 7092) se aplica sobre la renta total mensual, **sin restar la CCSS obrera**. Si es así, la retención está subestimada.

## 2. Hallazgos

### 2.1 Norma (Ley 7092, Título II)

- **Art. 33.** El patrono aplica la escala sobre la *renta total percibida mensualmente* por el trabajador. El Título II no contiene ningún artículo que permita restar cargas sociales antes de aplicar la escala.
- **Art. 34.** Los créditos por hijo y cónyuge se restan del **impuesto calculado**, no de la base. El código ya lo hace así: correcto.
- **Art. 35.** No están sujetos el aguinaldo (hasta 1/12 de los salarios del año), las indemnizaciones por muerte o incapacidad (régimen de seguridad social, INS o sentencia) y otras indemnizaciones del Código de Trabajo.
- **Art. 36.** En trabajos eventuales o discontinuos, el impuesto **y la cuota no sujeta** se prorratean por los días efectivamente trabajados.
- **Art. 38.** El tramo exento es uno solo. Con varios patronos, el trabajador debe notificarlo para que no se le aplique otra vez la cuota libre.
- **Art. 8 inciso i).** Las cuotas patronales son gasto deducible para la **empresa** en el impuesto sobre utilidades. No tiene relación con la base del trabajador.

Fuente consultada: texto consolidado de la Ley 7092 en Justia Costa Rica. Esa versión es **anterior a la reforma de la Ley 9635 (2018)**, que modificó los tramos del art. 33. Hay que confirmar la redacción vigente en SCIJ (pgrweb.go.cr/scij) y en el Reglamento vigente de la Ley del Impuesto sobre la Renta.

### 2.2 Práctica y fuentes secundarias (2026)

- Decreto 45333-H: tramos 2026 (exento hasta ₡918 000; 10 % hasta ₡1 347 000; 15 % hasta ₡2 364 000; 20 % hasta ₡4 727 000; 25 % sobre el exceso).
- Varias guías de 2026 (Alegra, Siempre al Día, aglegal, toptrabajos) indican que la escala se aplica sobre el **salario bruto** y que la CCSS obrera **no** reduce la base. Según Alegra, la tabla oficial de Hacienda (TramosRenta2026.pdf) rotula los tramos como «salario bruto».
- Hay fuentes discrepantes (Rivermate y Deel, ambas de EOR extranjeros) que afirman que la CCSS obrera es deducible. No citan norma. Se consideran de menor peso, pero hay que descartarlas formalmente con el contador.

## 3. Impacto

Ejemplo con bruto ₡1 500 000, sin créditos, CCSS obrera 10,83 %:

| Base | Cálculo | Renta mensual |
|---|---|---|
| Actual (bruto − CCSS = ₡1 337 550) | 10 % × 419 550 | ₡41 955 |
| Hipótesis (bruto ₡1 500 000) | 10 % × 429 000 + 15 % × 153 000 | ₡65 850 |
| **Diferencia** | | **₡23 895 de menos por mes** |

El patrono, como agente retenedor, responde solidariamente por lo no retenido (arts. 41 y 44): el impuesto, sus intereses y además pierde la deducibilidad del gasto. Producción tiene 0 nóminas, así que hoy no hay exposición; existirá desde la primera planilla real.

## 4. Preguntas para el contador

1. ¿La base del impuesto al salario es la renta total mensual (bruto), sin restar la CCSS obrera? ¿Hay alguna norma o resolución de la DGT que diga lo contrario?
2. ¿Qué conceptos del bruto no son sujetos y deben excluirse de la base? Por ejemplo: aguinaldo, subsidios por incapacidad pagados por CCSS/INS, viáticos, salario en especie (este último sí es gravable, art. 32 c).
3. Ausencias no pagadas (hoy se prorratea el bruto por horas): ¿se mantiene el tramo exento completo o aplica el prorrateo del art. 36? Entendemos que el art. 36 es solo para trabajo eventual o discontinuo.
4. Ingreso o salida a mitad de mes: ¿se prorratea la cuota no sujeta?
5. Empleado con otro patrono (art. 38): ¿cómo debe registrarse que no se le aplica el tramo exento?
6. ¿Los créditos por hijo y cónyuge se aplican siempre que el empleado los declare, y con qué respaldo documental?
7. Formulario y plazo de declaración y pago de la retención (D-103 o equivalente en TRIBU-CR) y su asiento contable.

## 5. Cambio previsto en el código (si se confirma)

- `baseGravable = brutoProrrateado` (sin restar `ccssObrero`), menos los ingresos no sujetos que confirme el contador.
- Prueba de referencia: bruto ₡1 500 000, sin créditos, tramos 2026 → ₡65 850.
- Se registra como ADR y tarea. No se toca nada antes de la validación.
