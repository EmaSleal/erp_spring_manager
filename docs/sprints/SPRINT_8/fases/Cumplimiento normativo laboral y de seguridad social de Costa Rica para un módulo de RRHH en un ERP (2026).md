# Cumplimiento normativo laboral y de seguridad social de Costa Rica para un módulo de RRHH en un ERP (2026)

## TL;DR
- Un módulo de RRHH costarricense debe soportar: contratos con contenido mínimo del Art. 24 del Código de Trabajo, vacaciones (Arts. 153-159), aguinaldo (Ley 2412), preaviso/cesantía (Arts. 28-29), ausencias/incapacidades (CCSS/INS), y —crítico— cálculo de deducciones de planilla (CCSS 10,83% obrero / 26,83% patronal en 2026), retención de impuesto al salario y aportes de la Ley de Protección al Trabajador (ROP 4,25% + FCL 1,5%).
- **La omisión del módulo de Nómina (Payroll) SÍ es un vacío crítico**: la mayor parte de las obligaciones legales costarricenses (planilla SICERE mensual, retención y declaración de impuesto al salario en TRIBU-CR, deducciones obrero-patronales, cálculo de aguinaldo, liquidaciones) viven en la nómina. Sin esa entidad, el ERP no puede cumplir la normativa aunque tenga bien modeladas Empleado y ContratoEmpleado.
- El modelo actual (Departamento, Puesto, Empleado, ContratoEmpleado, Ausencia) es una base razonable pero incompleta: faltan entidades/campos para Nómina, líneas de deducción, saldos de vacaciones, tipos de jornada y catálogos regulatorios versionables (categoría ocupacional del salario mínimo, operadora de pensiones, tipo de incapacidad, porcentajes de cargas y tramos de renta).

## Key Findings

1. **Tipos de contrato**: Costa Rica reconoce contrato por tiempo indefinido (regla general), a plazo fijo/tiempo determinado (Arts. 26-27), por obra determinada, a tiempo parcial y aprendizaje; `ContratoEmpleado` debe modelar el tipo como enum y validar las reglas asociadas.
2. **Contenido mínimo del contrato**: el Art. 24 CT enumera datos obligatorios (partes, cédula, tipo/duración, jornada, salario y forma de pago, lugar de trabajo). Los contratos que exceden ciertos plazos deben ser escritos (Arts. 22-23).
3. **Vacaciones**: mínimo de 2 semanas por cada 50 semanas trabajadas (Art. 153); en la práctica se acumula ~1 día por mes; pago proporcional obligatorio en liquidación.
4. **Aguinaldo**: 1/12 de lo devengado entre 1 dic y 30 nov, pago máximo el 20 de diciembre (Ley 2412), exento de cargas sociales e impuesto.
5. **Cesantía/preaviso**: escala progresiva del Art. 29 (19,5 a 22 días/año), tope 8 años; preaviso Art. 28 (1 semana a 1 mes según antigüedad).
6. **Deducciones de planilla 2026**: obrero 10,83% (SEM 5,50% + IVM 4,33% + Banco Popular 1%); patronal 26,83%; más impuesto al salario progresivo (exento hasta ₡918.000).
7. **Nómina como vacío crítico**: la ausencia de entidad Nómina impide cumplir con SICERE, retenciones tributarias, aguinaldo y liquidaciones.

## Details

### 1. Tipos de contrato laboral y requisitos mínimos

**Base legal:** Código de Trabajo (Ley N.º 2 de 1943), Arts. 18-27.

El contrato individual de trabajo (Art. 18) existe cuando concurren tres elementos: prestación personal, remuneración (salario) y subordinación jurídica. Rige el principio de "primacía de la realidad": si existen esos tres elementos, hay relación laboral aunque el documento diga "servicios profesionales".

Tipos reconocidos:
- **Tiempo indefinido**: regla general, sin fecha de terminación. Es la forma recomendada para labores permanentes.
- **Plazo fijo / tiempo determinado** (Arts. 26-27): solo válido cuando la naturaleza del servicio lo justifique (sustitución, proyecto temporal, temporada). Duración general máxima de 12 meses, o 60 meses si requiere preparación técnica especial. Prohibido para tareas de carácter permanente.
- **Por obra determinada** (Art. 26): la relación termina al completarse la obra.
- **A tiempo parcial**: proporcional en salario y cargas.
- **Aprendizaje** y **contrato colectivo/convención colectiva** (Arts. 49-57).

Forma (Arts. 22-23): puede ser verbal solo en casos específicos (labores agrícolas/ganaderas, trabajos accidentales/temporales que no excedan 90 días, obra de bajo valor). En los demás casos debe ser escrito, en tres tantos, uno de los cuales el patrono remite a la Oficina de Empleo del MTSS dentro de los 15 días posteriores.

**Contenido mínimo (Art. 24 CT)** — el contrato escrito contendrá:
a) nombres, apellidos, nacionalidad, edad, sexo, estado civil y domicilio de los contratantes;
b) número de cédula de identidad;
c) residencia del trabajador cuando se le contrate para lugar distinto del habitual;
d) duración del contrato o expresión de si es por tiempo indefinido, para obra determinada o a precio alzado;
e) tiempo de la jornada y horas en que se ejecutará;
f) el sueldo/salario/jornal, forma de cálculo (por unidad de tiempo, por obra), y forma, período y lugar de pago;
g) lugar(es) donde se prestará el servicio;
h) demás estipulaciones acordadas.
Período de prueba: 3 meses (durante los cuales el patrono puede rescindir sin responsabilidad).

**Mapeo al modelo:** `ContratoEmpleado` debe incluir: `tipoContrato` (enum: INDEFINIDO, PLAZO_FIJO, OBRA_DETERMINADA, TIEMPO_PARCIAL, APRENDIZAJE), `fechaInicio`, `fechaFin` (nullable), `justificacionTemporalidad` (obligatorio si plazo fijo/obra), `jornadaTipo`, `salarioPactado`, `formaPago` (unidad de tiempo/obra/comisión), `periodicidadPago`, `lugarTrabajo`, `fechaFinPeriodoPrueba`. Los datos de las partes (cédula, nacionalidad, estado civil, domicilio) deben residir en `Empleado`.

### 2. Vacaciones

**Base legal:** Arts. 153-161 CT.

- **Derecho:** mínimo 2 semanas de vacaciones remuneradas por cada 50 semanas de labores continuas (Art. 153). En jornada de 6 días equivale a 12 días hábiles; en jornada de 5 días, a 10 días hábiles.
- **Acumulación proporcional:** en la práctica se genera ~1 día por mes trabajado. El disfrute proporcional antes de cumplir las 50 semanas no está permitido, pero si el contrato termina antes, se paga 1 día por cada mes trabajado en la liquidación (Art. 156).
- **Época de disfrute** (Art. 155): el patrono la fija dentro de las 15 semanas siguientes al cumplimiento de las 50 semanas.
- **Fraccionamiento** (Art. 158): máximo 2 fracciones, por acuerdo de partes.
- **Prohibición de acumulación** (Art. 159): no se pueden acumular salvo labores técnicas/de confianza, y solo por un período.
- **Cálculo del pago** (Art. 157): promedio de salarios ordinarios y extraordinarios de las últimas 50 semanas.
- Los períodos de suspensión del contrato (incapacidad, permiso sin goce) no acumulan vacaciones ni cuentan para las 50 semanas.

**Mapeo al modelo:** se requiere una entidad `SaldoVacaciones` o campos en Empleado que registren `fechaIngreso`, `diasGenerados`, `diasDisfrutados`, `diasPagados`, `saldoActual`. La entidad `Ausencia` con tipo VACACIONES debe descontar del saldo. El pago requiere acceso al histórico salarial (Nómina).

### 3. Aguinaldo (décimo tercer mes)

**Base legal:** Ley N.º 2412 (Ley del Aguinaldo en la Empresa Privada, 1959).

- **Fórmula:** suma de todos los salarios ordinarios y extraordinarios brutos devengados entre el 1 de diciembre del año anterior y el 30 de noviembre del año en curso, dividido entre 12.
- **Período de acumulación:** 1 dic – 30 nov (fijo por ley).
- **Fecha límite de pago:** dentro de los primeros 20 días de diciembre (a más tardar el 20 de diciembre).
- **Incluye:** horas extra, comisiones, bonificaciones, feriados trabajados, salario en especie.
- **Excluye:** subsidios de incapacidad de CCSS/INS (no son salario); la licencia de maternidad SÍ computa.
- **Exención:** el aguinaldo está exento de cargas sociales e impuesto sobre la renta.
- **Proporcional:** si la relación termina antes de diciembre, se paga el proporcional acumulado.

**Mapeo al modelo:** requiere entidad Nómina con histórico mensual de devengos para acumular la base dic-nov. Debe distinguir conceptos salariales (computan) de subsidios (no computan).

### 4. Cesantía y preaviso

**Base legal:** Arts. 28, 29, 30 CT; Ley de Protección al Trabajador N.º 7983 (tope 8 años, FCL).

**Preaviso (Art. 28):**
- 3 a 6 meses trabajados: 1 semana.
- 6 meses a 1 año: 15 días.
- Más de 1 año: 1 mes.
- (Menos de 3 meses: no aplica.) Aplica tanto a renuncia como a despido con responsabilidad patronal. Puede pagarse en dinero.

**Cesantía (Art. 29)** — escala progresiva por año, tope 8 años, base = promedio de salarios de los últimos 6 meses:
- Año 1: 19,5 días
- Año 2: 20 días
- Año 3: 20,5 días
- Año 4: 21 días
- Año 5: 21,24 días
- Año 6: 21,5 días
- Año 7: 22 días
- Año 8: 22 días
- Año 9: 22 días
- Año 10: 21,5 días
- Año 11: 21 días
- Año 12: 20,5 días
- Año 13 y siguientes: 20 días
- Fracción superior a 6 meses en el último tramo cuenta como año adicional.
- Tope: no más de 8 años reconocidos (~172 días máximo).
- La cesantía es exenta de cargas sociales e impuesto, y solo puede deducirse por pensión alimentaria.

**Causales:**
- Despido **con** responsabilidad patronal (sin justa causa, despido indirecto, causas ajenas al trabajador): paga preaviso + cesantía.
- Despido **sin** responsabilidad patronal (Art. 81, falta grave): no paga preaviso ni cesantía.
- Renuncia voluntaria: solo vacaciones y aguinaldo proporcionales (el trabajador debe dar el preaviso).

**Mapeo al modelo:** requiere módulo de Liquidación (parte de Nómina) que calcule según antigüedad (derivada de `fechaIngreso` en ContratoEmpleado) y el histórico salarial de 6 meses. Campo `causaTerminacion` (enum) y `fechaTerminacion` en ContratoEmpleado.

### 5. Ausencias, incapacidades y licencias

**Incapacidad por enfermedad común (CCSS/SEM):** los primeros 3 días el patrono paga el 50% y la CCSS el 50%; a partir del día 4 la CCSS paga el 60% del salario promedio de las últimas planillas y el patrono queda exento (puede complementar voluntariamente). Requiere boleta médica electrónica (EDUS) de la CCSS. Requisito: haber cotizado al menos 3 de los últimos 6 meses.

**Riesgos del trabajo (INS):** accidentes/enfermedades laborales cubiertos por la póliza de Riesgos del Trabajo del INS (Ley 6727). El INS paga el 60% del salario los primeros 45 días; a partir del día 46 se reajusta. El patrono debe reportar el accidente en RT Virtual dentro de 8 días hábiles. El patrono no está obligado a complementar.

**Licencia de maternidad (Art. 95 CT):** 4 meses (1 antes del parto + 3 después), pagada 50% patrono / 50% CCSS, al 100% del salario. Protección contra despido. Sí computa como salario para aguinaldo y antigüedad.

**Licencia de paternidad (Ley N.º 10.211, vigente desde jun 2022):** 8 días para el padre biológico, distribuidos 2 días por semana durante las primeras 4 semanas posteriores al nacimiento.

**Licencia por duelo:** 2 días con goce de salario por fallecimiento de ciertos parientes cercanos.

**Vacaciones, permisos con y sin goce de salario:** los permisos sin goce suspenden el contrato (no acumulan vacaciones ni antigüedad).

**Quién aprueba/certifica:** las incapacidades las certifica la CCSS (médico) o el INS (riesgos laborales); las licencias de maternidad/paternidad se sustentan con constancia de nacimiento/PANI; los permisos con o sin goce los aprueba el patrono.

**Mapeo al modelo:** la entidad `Ausencia` debe tener `tipoAusencia` (enum: INCAPACIDAD_CCSS, INCAPACIDAD_INS, MATERNIDAD, PATERNIDAD, DUELO, VACACIONES, PERMISO_CON_GOCE, PERMISO_SIN_GOCE), `fechaInicio`, `fechaFin`, `entidadCertificante`, `numeroBoleta`, `porcentajePatrono`, `porcentajeSubsidio`, `computaParaAguinaldo` (bool), `computaAntiguedad` (bool). Esto es esencial porque cada tipo afecta distinto a la nómina, el aguinaldo y las vacaciones.

### 6. Deducciones obligatorias de planilla (2026)

Según AG Legal (dic. 2025), las cargas sociales totales vigentes desde el 1 de enero de 2026 son **26,83% (patronal) y 10,83% (obrera)**, con **25,33% para patronos no agrícolas con menos de 5 empleados**; las tasas de IVM son válidas hasta el 31 de diciembre de 2028.

**Cargas sociales — aporte del TRABAJADOR = 10,83%:**
- SEM (Seguro de Salud y Maternidad): 5,50%
- IVM (Invalidez, Vejez y Muerte): 4,33%
- Banco Popular (cuota obrera): 1,00%

El aumento del IVM lo aprobó la CCSS por el Transitorio XI del Reglamento del Seguro de IVM (acuerdo de Junta Directiva de 2019, Acta N.º 9038). Según La Nación (dic. 2025): *"Los asalariados pasarán de aportar al IVM un 4,17% a un 4,33% de sus sueldos... Este nuevo porcentaje regirá hasta el 31 de diciembre de 2028 y, a partir del 1.° de enero del 2029, los aportes volverán a incrementarse a un total de 12,16%."*

**Cargas sociales patronales = 26,83%:**
- SEM: 9,25%
- IVM: 5,58%
- FCL (Fondo de Capitalización Laboral): 1,50%
- OPC (Operadora de Pensiones Complementaria / ROP): 2,00%
- FODESAF: 5,00%
- IMAS: 0,50%
- INA: 1,50% (exento para patronos no agrícolas con menos de 5 empleados → total 25,33%)
- Banco Popular patronal: 0,25%
- Banco Popular Ley Protección al Trabajador: 0,25%
- INS Riesgos del Trabajo: variable según actividad (se suma aparte)

Según La Nación: *"Los patronos aumentarán el pago de sus cotizaciones al régimen de pensiones del 5,42% al 5,58%. El aumento del Estado pasará de un 1,57% a 1,75%."* El ajuste afecta a cerca de 1,4 millones de personas cotizantes al IVM (dato CCSS).

**Base Mínima Contributiva (BMC) 2026:** salarios inferiores a ₡333.328 (SEM) o ₡311.990 (IVM) se ajustan a esa base.

**Impuesto sobre la renta al salario (Ley 7092; Decreto Ejecutivo N.º 45333-H, La Gaceta N.º 229 del 5 dic 2025, vigente 1 ene 2026), retención mensual progresiva:**
- Hasta ₡918.000: exento
- De ₡918.000 a ₡1.347.000: 10%
- De ₡1.347.000 a ₡2.364.000: 15%
- De ₡2.364.000 a ₡4.727.000: 20%
- Sobre el exceso de ₡4.727.000: 25%
- Créditos fiscales 2026: ₡1.710 mensuales por hijo; ₡2.590 mensuales por cónyuge (Siempre al Día).
- El impuesto al salario y la cuota CCSS del trabajador se calculan independientemente sobre el salario bruto (uno no reduce la base del otro).

El umbral se ajustó por una variación negativa del IPC. Según ECIJA, citando al socio Daniel Valverde: *"los tramos de renta tienen una ligera rebaja del -0,38%. Por tal razón, ahora el pago del impuesto de la renta inicia para salarios de ₡918.000, frente a los ₡922.000 que estaba vigente este 2025."* (variación IPC del INEC: 108,847 en oct 2024 → 108,436 en oct 2025).

**Asociaciones solidaristas y otros:** ahorro obligatorio solidarista (3%-5% del salario bruto), cuotas sindicales/cooperativas, embargos, pensión alimentaria.

**Orden de prelación de rebajos** (respaldado por BLP Legal y práctica consolidada; la PGR advierte que no hay una única ley cerrada): 1) cargas sociales CCSS; 2) impuesto al salario; 3) ahorro obligatorio de asociación solidarista; 4) pensión alimentaria (carácter preferente, hasta 50% del salario, con prioridad sobre embargos civiles — Art. 64 Ley 7654); 5) demás embargos y deducciones convencionales. Existe salario mínimo inembargable (Arts. 172-174 CT; regla de 1/8 y 1/4 sobre el excedente, salvo pensión alimentaria).

**Mapeo al modelo:** requiere entidad `Nomina`/`LineaNomina` con líneas de deducción parametrizables por porcentaje y tope, catálogo de porcentajes versionable por vigencia, y campos en Empleado para `numeroAseguradoCCSS`, `operadoraPensiones`, `cargasFamiliares` (para créditos fiscales), `afiliacionSolidarista`, `ordenAlimentario`.

### 7. Reportes/entregables obligatorios a entidades reguladoras

**Planilla CCSS (SICERE / Oficina Virtual):** reporte MENSUAL de salarios, trabajadores activos, altas/bajas, movimientos, incapacidades y licencias. Presentación del 26 de cada mes al cuarto día hábil del mes siguiente (grandes clientes: tercer día hábil). Pago del 16 al 20 de cada mes. El atraso genera recargos. Los aportes de ROP y FCL se recaudan por esta misma vía.

**Reportes tributarios a Hacienda (TRIBU-CR, sistema vigente desde el 6 oct 2025 que reemplazó al ATV):**
- **Autoliquidación del impuesto al salario retenido** (antes D-103/D-103-1; ahora Formulario 138 para persona jurídica, 137 persona física, 139 persona pública, Serie 100): MENSUAL, dentro de los primeros 15 días naturales del mes siguiente. El patrono es agente de retención con responsabilidad solidaria (Arts. 23-24 Ley 7092).
- **Declaración informativa de detalle de salarios/retenciones por empleado** (antes D-152 anual; ahora Formulario 208 PJ / 207 PF / 209 PP, Serie 200): ahora MENSUAL, mismos 15 días naturales; se presenta primero y genera automáticamente la autoliquidación 138.
- **D-151 (ahora D-270)**: informativa de clientes/proveedores/gastos; **los salarios están EXCLUIDOS** de este reporte (se informan por la vía anterior).

Nota de fiabilidad: TRIBU-CR es muy reciente y sus resoluciones se han reformado varias veces en 2026 (p. ej. MH-DGT-RES-0010-2026 cambió el plazo del D-270); los códigos de formulario y plazos exactos deben confirmarse en ovitribucr.hacienda.go.cr antes de implementar.

**Aguinaldo:** no tiene un reporte regulatorio específico, pero su pago debe documentarse.

**Mapeo al modelo:** el ERP debe poder generar el archivo de planilla en el formato SICERE y los datos para las declaraciones de TRIBU-CR (detalle por empleado: identificación, monto bruto, retención). Esto es imposible sin una entidad Nómina.

### 8. Jornada laboral

**Base legal:** Constitución Art. 58; Código de Trabajo Arts. 135-145.

- **Jornada diurna** (5:00-19:00): máx. 8 h/día y 48 h/semana; extensible a 10 h/día en labores no insalubres/peligrosas sin superar 48 h/semana.
- **Jornada nocturna** (19:00-5:00): máx. 6 h/día y 36 h/semana.
- **Jornada mixta**: máx. 7 h/día y 42 h/semana; extensible a 8 h en labores no peligrosas. Se convierte en nocturna si incluye 3,5 h o más entre las 19:00 y 5:00.
- **Horas extra (Art. 139):** el trabajo fuera de la jornada ordinaria se paga con recargo del 50% (tiempo y medio). Límite: la jornada total (ordinaria + extra) no puede exceder 12 h/día.
- **Trabajadores de confianza (Art. 143):** pueden laborar hasta 12 h sin pago de horas extra.
- Valor de la hora ordinaria mixta ≈ 1,14× la diurna; nocturna ≈ 1,33× la diurna (para igualar el salario por jornada).

**Mapeo al modelo:** `Puesto` o `ContratoEmpleado` debe tener `tipoJornada` (enum: DIURNA, NOCTURNA, MIXTA) y `horasSemanales`. El cálculo de horas extra pertenece a Nómina y depende de Asistencia (excluida del sprint) — esto crea una dependencia futura que debe preverse.

### 9. Salario mínimo 2026

**Base legal:** Ley N.º 832; Decreto Ejecutivo N.º 45303-MTSS (La Gaceta N.º 229, Alcance N.º 156, 5 dic 2025, vigente 1 ene 2026); aprobado en el Consejo Nacional de Salarios, sesión ordinaria N.º 5886 del 27 oct 2025.

- **Aumento general:** 1,63% para las categorías genéricas. Según El Financiero: *"El aumento general será del 1,63%, el más bajo de la administración actual."* Ajustes diferenciados: trabajo doméstico +3,96%, ocupaciones especializadas +2,18%, técnico medio en educación diversificada +2,50%.
- **Estructura por categoría ocupacional** (siglas TONC, TOSC, TOC, TES, TOE): el sistema clasifica cientos de oficios. Según Alegra (con base en el Decreto 45303-MTSS): *"Los montos van desde ₡268.731,31 mensuales para trabajo doméstico hasta ₡796.921,00 para licenciados universitarios."* El Trabajador No Calificado Genérico (TONC) es ≈ ₡373.092,30/mes.
- Se actualiza normalmente dos veces al año (enero y julio) por el CNS.
- Pagar por debajo del mínimo es infracción (Arts. 177, 611 CT), con multas.

**Mapeo al modelo:** `Puesto` debe tener `categoriaSalarialMinima` (código del decreto MTSS) y el sistema debe validar que `salarioBase`/`salarioPactado` ≥ mínimo de la categoría. Se requiere catálogo de salarios mínimos versionable por vigencia.

### 10. Otros requisitos normativos (Ley de Protección al Trabajador, ROP, FCL)

**Base legal:** Ley de Protección al Trabajador N.º 7983 (2000) y reformas (Ley 9906).

- **ROP (Régimen Obligatorio de Pensiones Complementarias):** afiliación obligatoria; aporte total 4,25% del salario, compuesto por 3,25% patronal y 1% del trabajador. Administrado por una Operadora de Pensiones Complementarias (OPC) elegida por el trabajador.
- **FCL (Fondo de Capitalización Laboral):** aporte patronal del 1,5% del salario mensual; ahorro para cesantía/desempleo retirable cada 5 años o al terminar la relación.
- El trabajador debe elegir OPC; si no lo hace, se le asigna una por defecto (estado automático, OPC-CCSS).
- Estos aportes se recaudan vía CCSS/SICERE junto con las demás cargas.

**Mapeo al modelo:** `Empleado` debe registrar `operadoraPensiones` (OPC) y el sistema debe calcular ROP y FCL como parte de las cargas patronales en Nómina.

## El vacío crítico: la ausencia del módulo de Nómina

La observación del usuario es correcta y merece elevarse a **riesgo bloqueante de cumplimiento**. El diseño actual trata "salarioBase" y "salarioPactado" como campos que "alimentan a Nómina" sin modelar esa entidad. Sin embargo, **la abrumadora mayoría de las obligaciones legales costarricenses se materializan en la nómina**, no en el contrato ni en el empleado:

- El cálculo y retención de deducciones obrero-patronales (CCSS 10,83% / 26,83%), impuesto al salario, ROP y FCL.
- La generación de la planilla mensual SICERE (obligación legal con plazos y sanciones).
- Las declaraciones tributarias mensuales en TRIBU-CR (formularios 138/208).
- El cálculo del aguinaldo (requiere histórico de devengos dic-nov).
- La liquidación laboral (preaviso, cesantía, vacaciones y aguinaldo proporcionales).
- El pago de horas extra, incapacidades (subsidios y complementos) y licencias.

Sin una entidad `Nomina` (con `LineaNomina`/`DetalleDeduccion` y un histórico mensual de devengos por empleado), el ERP no puede cumplir ninguna de estas obligaciones. Modelar Empleado y ContratoEmpleado sin Nómina es tener el "quién" y el "cuánto pactado" pero no el "cuánto se pagó, se retuvo y se reportó" —que es precisamente lo que la ley exige documentar.

## Recommendations

**Etapa 1 — cerrar el alcance del Sprint 8 sin comprometer el cumplimiento futuro:**
1. Aunque Nómina se implemente después, **defina desde ya el contrato de datos (interfaces/DTO) que Nómina consumirá** de Empleado, ContratoEmpleado y Ausencia, para no rehacer el modelo. Documente explícitamente que el cumplimiento normativo depende de un módulo Nómina posterior (registrar como deuda técnica y riesgo de compliance en el backlog).
2. Enriquezca `ContratoEmpleado` con: `tipoContrato`, `justificacionTemporalidad`, `tipoJornada`, `horasSemanales`, `formaPago`, `periodicidadPago`, `causaTerminacion`, `fechaTerminacion`, `fechaFinPeriodoPrueba`.
3. Enriquezca `Empleado` con los campos regulatorios: `cedula`/tipo de identificación, `numeroAseguradoCCSS`, `operadoraPensionesROP`, `cargasFamiliares`, `afiliacionSolidarista`, `ordenPensionAlimentaria`.
4. Enriquezca `Puesto` con `categoriaSalarialMinima` (código decreto MTSS) y valide `salarioBase ≥ mínimo`.
5. Robustezca `Ausencia` con los campos de tipo, entidad certificante, boleta, porcentajes y flags de cómputo (aguinaldo/antigüedad/vacaciones).

**Etapa 2 — catálogos regulatorios versionables:**
6. Cree catálogos con vigencia (fecha desde/hasta) para: porcentajes de cargas sociales, tramos de impuesto al salario, salarios mínimos por categoría, y BMC. Nunca "hardcodear" porcentajes: la ley los cambia (el IVM subió el 1 ene 2026 y volverá a subir en 2029; los tramos de renta se ajustan cada año por IPC; el salario mínimo, cada semestre).

**Etapa 3 — módulo Nómina (sprint siguiente, prioritario):**
7. Implemente `Nomina` + `LineaNomina` + `HistoricoDevengos` que soporten: cálculo de deducciones, aguinaldo, liquidación, y generación de archivos SICERE y datos TRIBU-CR (formularios 138/208).
8. Prevea la integración futura con Asistencia (horas extra) y con los reportes regulatorios (SICERE mensual y declaraciones TRIBU-CR).

**Benchmarks que cambian las recomendaciones:** si el ERP se comercializa a empresas con planilla real, Nómina deja de ser opcional y se vuelve requisito de lanzamiento (MVP legal). Si el sistema solo gestiona expedientes de RRHH sin pagar salarios, el vacío de Nómina es tolerable pero debe integrarse con un software de planilla externo vía API.

## Caveats
- Los porcentajes de cargas sociales (IVM) rigen del 1 ene 2026 al 31 dic 2028; volverán a subir en 2029 (aporte tripartito total del IVM al 12,16%). El salario mínimo se ajusta semestralmente (enero y julio). Los tramos de renta se actualizan anualmente por IPC (el de 2026 se ajustó -0,38%).
- TRIBU-CR es muy reciente (oct 2025) y sus resoluciones se han reformado varias veces en 2026; las fechas exactas de vencimiento y los códigos de formulario (138/208/D-270) deben confirmarse en ovitribucr.hacienda.go.cr antes de implementar.
- Varias cifras provienen de firmas legales/contables reputadas (AG Legal, Alegra/Siempre al Día, BLP, ECIJA, El Financiero, La Nación) que citan los decretos oficiales; para producción, validar contra los textos oficiales del MTSS, CCSS, Hacienda y la PGR (SCIJ), y contra los PDF de los decretos 45303-MTSS y 45333-H.
- Este informe cubre el régimen del sector privado; el sector público y regímenes especiales (trabajo doméstico, agrícola) tienen reglas particulares.
- La distribución interna del ROP (3,25% patronal + 1% obrero) proviene de la Ley 7983 y sus reformas (Ley 9906); confirmar la composición vigente exacta con SUPEN, pues ha habido ajustes en la ruta de los aportes (paso temporal por el Banco Popular).