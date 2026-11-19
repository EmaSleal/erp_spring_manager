# Envío a Hacienda en etapas cortas (ADR-043)

Estado: aceptado el 2026-11-17; implementado en T-037 (ver «Implementación»). Resuelve P-076 (envío dentro del request) y P-089 (consecutivo sin bloqueo), y se suma P-088 (falso éxito al renovar el token). Complementa ADR-030 (mutabilidad fiscal y estados) y ADR-031.

## Problema

- `ComprobanteElectronicoServiceImpl.procesarFactura` es `@Transactional`: genera, toma el consecutivo, firma, envía y luego llama a `procesarFacturaAsync`, que espera hasta 5 × 30 s con `Thread.sleep` consultando el estado. Todo pasa en la misma transacción y en el hilo de la petición. El `@Async` no actúa, porque no hay `@EnableAsync` y la llamada es una auto-invocación.
- La nota de crédito (`NotaCreditoRestController.emitir`) llama a `procesarFacturaAsync` con el mismo efecto.
- `ConfiguracionHacienda.siguienteConsecutivo` incrementa un campo con `synchronized` sobre la instancia, sin `@Version` y sin bloqueo en la BD. Dos emisiones solapadas toman el mismo número y la última escritura gana. Como las claves difieren por el código de seguridad aleatorio, ninguna restricción lo detecta, y Hacienda responde -99.
- Un rollback de esa transacción larga después de un POST exitoso perdería el registro de un comprobante que Hacienda ya tiene.

## Diseño

### Un único servicio de envío
Factura, nota de crédito, reenvío manual y `ComprobanteReintentosJob` usan el mismo servicio. Ningún otro código llama a `HaciendaApiService.enviarComprobante`.

### Etapas
| Etapa | Transacción | Qué hace | Resultado |
|---|---|---|---|
| a. Preparación | propia, corta | validaciones (cliente, CAByS, vigente), consecutivo con `PESSIMISTIC_WRITE` sobre `configuracion_hacienda`, generación, firma, XSD (T-036) | FIRMADO, o ERROR local (ADR-030) |
| b. Marca | propia, corta | `UPDATE ... SET estado='ENVIO_INCIERTO', fecha_envio=?, intentos_envio=intentos_envio+1 WHERE id=? AND vigente AND estado IN (GENERADO, FIRMADO, ERROR, NO_TRANSMITIDO)` | 1 fila: sigue; 0 filas: otro proceso envía, no se transmite |
| c. POST | ninguna | token (fuera de la transacción del comprobante) y POST de recepción | respuesta clasificada en origen (T-025/T-033) |
| d. Registro | propia, corta | guarda ENVIADO, NO_TRANSMITIDO o ENVIO_INCIERTO, con código y mensaje | estado final del envío |
| e. Sondeo | una por consulta | `TaskScheduler`: +30 s, hasta 5 consultas, con `consultarYActualizarEstado` | ACEPTADO o RECHAZADO; si no, sigue el job horario |

La petición del usuario termina después de la etapa d. Si una etapa falla, no se ejecutan las siguientes: con un fallo en a no hay marca, y con un fallo en b no hay POST.

### Marca previa al POST = ENVIO_INCIERTO
- Mientras dura el POST, el comprobante está en ENVIO_INCIERTO. `puedeReenviar` ya lo excluye: no hay reenvío manual concurrente.
- Si el proceso cae entre b y d, el comprobante queda en ENVIO_INCIERTO y aplica ADR-030 §2: consultar; un 404 pasados `MINUTOS_ANTES_DE_VERIFICAR` (15) → NO_TRANSMITIDO → reenviable con la misma clave.
- La interfaz muestra «Enviando…» si el estado es ENVIO_INCIERTO y `fecha_envio` tiene menos de 2 minutos; pasado ese tiempo, «Envío incierto».

### Timeouts y reintentos
- El POST de recepción no se reintenta automáticamente ante un timeout o un error de E/S (Resilience4j `haciendaAPI` no aplica a `enviarComprobante`, o se excluye esa operación). Un timeout después de enviar deja ENVIO_INCIERTO.
- Los timeouts de conexión y lectura del POST y del token quedan configurados y documentados. El peor caso de la petición del usuario (token + POST) es conocido y menor de 60 s.

### Consecutivo
- Se lee y se incrementa con un bloqueo de fila (`@Lock(PESSIMISTIC_WRITE)` en el repositorio) dentro de la etapa a. Se elimina `synchronized`.
- Una restricción única en `comprobante_electronico` (empresa, ambiente, tipo, consecutivo), creada por una migración, falla antes de transmitir si algo se escapa. La migración verifica que los datos actuales no la violen y, si los violan, lo informa.

### Sin @EnableAsync
Se quita el `@Async` sin efecto del envío (`procesarFacturaAsync` se reemplaza por el servicio de envío). Las notificaciones `@Async` quedan como están (P-090).

### Errores al usuario
- Emisión y nota: mensajes fijos por resultado («Recibido por Hacienda», «No se pudo transmitir; se reintentará», «Envío incierto; se verificará con Hacienda», «XML inválido: vea el detalle»), sin `e.getMessage()` (P-070).
- Renovar token: 502 con un mensaje fijo si la autenticación falla (P-088).

## Implementación (T-037)

### Clases
| Pieza | Clase | Qué hace |
|---|---|---|
| Servicio único de envío | `EnvioHaciendaService` / `EnvioHaciendaServiceImpl` (`modules/facturacion/electronica/service`) | `emitirFactura`, `enviar`, `reenviar`, `registrarFalloDeReintento`. Lo usan `ComprobanteElectronicoController` (procesar, enviar, reenviar), `NotaCreditoRestController` y `ComprobanteReintentosJob`. `EnvioHaciendaEstructuraTest` verifica que ninguna otra clase llama a `HaciendaApiService.enviarComprobante`. |
| Etapa a (factura nueva) | `ComprobanteElectronicoService.generarParaEmision` | Cliente, CAByS (409 sin consumir consecutivo), vigente, consecutivo con bloqueo, clave y XML, en una transacción. |
| Etapa a (comprobante existente) | `ComprobanteElectronicoService.prepararParaEnvio` sobre la fila cargada con `ComprobanteElectronicoRepository.findByIdParaEnvio` (`@Lock(PESSIMISTIC_WRITE)`) | Regenera y firma si hace falta, valida contra el XSD (T-036) y comprueba la configuración; una falla local deja ERROR (ADR-030). |
| Consecutivo | `ConsecutivoHacienda` / `ConsecutivoHaciendaJpa` | `em.refresh(config, PESSIMISTIC_WRITE)` = `SELECT ... FOR UPDATE` sobre configuracion_hacienda dentro de la etapa a; relee el valor de la BD. `ConfiguracionHacienda.siguienteConsecutivo` ya no es `synchronized`: es solo el cálculo. |
| Etapa b | `ComprobanteElectronicoRepository.marcarEnvioEnCurso` | `UPDATE ... SET estado = ENVIO_INCIERTO, fecha_envio, intentos_envio + 1 WHERE id AND vigente AND estado IN (GENERADO, FIRMADO, ERROR, NO_TRANSMITIDO)`; 0 filas → no se transmite. |
| Etapa c | `HaciendaApiService.enviarComprobante` | Token + POST; `TransactionTemplate` con `PROPAGATION_NOT_SUPPORTED`: corre sin transacción aunque el llamador tenga una. |
| Etapa d | `EnvioHaciendaServiceImpl.registrar` | Guarda ENVIADO, NO_TRANSMITIDO o ENVIO_INCIERTO solo si el comprobante sigue en la marca. Si falla, queda ENVIO_INCIERTO (nunca un estado transmisible). Una excepción no clasificada del POST deja ENVIO_INCIERTO con un mensaje fijo. |
| Etapa e | `EnvioHaciendaServiceImpl.programarSondeo` / `consultar` + `taskScheduler` (`SchedulingConfig`) | +30 s, hasta 5 consultas, cada una en la transacción de `consultarYActualizarEstado`; se detiene en ACEPTADO, RECHAZADO, ANULADO o ENVIO_INCIERTO. Propiedades `facturacion.hacienda.sondeo.espera` (30s) y `.max-consultas` (5). |
| Mensajes | `MensajesEnvioHacienda` | Recibido / no transmitido / envío incierto / XML inválido (422) / error local (422) / error inesperado (500), sin texto de excepción. Las reglas del negocio usan `EmisionNoPermitidaException` (409, mensaje de la aplicación). |
| Restricción única | `V43__comprobante_consecutivo_unico` (migración Java) | Agrega `comprobante_electronico.ambiente`, lo completa con el de la configuración activa, verifica repetidos (si hay, se detiene y los lista) y crea `uk_comprobante_consecutivo (empresa_id, ambiente, tipo_comprobante, consecutivo)`. |
| «Enviando…» | `static/modules/facturacion/electronica/js/estado-envio.js` | ENVIO_INCIERTO con fecha_envio de menos de 2 min → «Enviando…»; después, «Envío incierto». Listado y detalle de comprobantes. |
| Renovar token (P-088) | `ConfiguracionHaciendaController.renovarToken` | 200 solo con un token nuevo y vigente; si no, 502 con mensaje fijo. |

Se eliminaron `procesarFacturaAsync` (con su `@Async` sin efecto y el `Thread.sleep` de 5 × 30 s), `ComprobanteElectronicoService.enviarAHacienda` y `reenviar`.

### Timeouts, reintentos y peor caso
- Cliente HTTP propio de Hacienda: bean `haciendaRestTemplate` (`HttpClientConfig`), usado por el token, la recepción y la consulta. Antes se compartía el bean genérico `restTemplate`, que `WhatsAppRestConfig` también define (con sobrescritura de beans habilitada).
  - `facturacion.hacienda.http.connect-timeout`: **5 s** (`HACIENDA_HTTP_CONNECT_TIMEOUT`).
  - `facturacion.hacienda.http.read-timeout`: **15 s** (`HACIENDA_HTTP_READ_TIMEOUT`).
- Resilience4j `haciendaAPI`:
  - **Retry**: solo en `consultarComprobante` (3 intentos, 2 s × 2). `enviarComprobante` no tiene `@Retry` (prueba `retrySoloEnLaConsulta`), así que un timeout de lectura deja ENVIO_INCIERTO tras una sola invocación (prueba `timeoutDeLectura_unaSolaInvocacion_envioIncierto`).
  - **Circuit breaker**: en `enviarComprobante` (circuito abierto → NO_TRANSMITIDO, sin llamada) y en `consultarComprobante`. No cambia en T-037.
- **Peor caso de la petición del usuario al emitir** (factura, nota o reenvío). Las etapas a, b y d son operaciones cortas de BD. El token es una petición al IdP: `enviarComprobante` llama a `autenticar` dentro de la misma clase, así que no usa la caché. El POST es otra petición. Total: 2 × (5 s + 15 s) = **40 s < 60 s**. A eso se suma la resolución DNS del sistema operativo, que los timeouts de la JVM no acotan.
- La consulta manual de estado (no es parte de la emisión) puede llegar a 3 × 20 s + 2 s + 4 s = 66 s por el retry de la consulta. Queda igual que antes.

### Constantes por ambiente (review 1, P-091)
Todas las constantes del cliente de Hacienda que dependen del ambiente están en `AmbienteHacienda`. Ningún otro archivo tiene literales de client_id, realm o URL de Hacienda (`EnvioHaciendaEstructuraTest.constantesDelAmbienteSoloEnElEnum`). `autenticar` (grant_type password) y `renovarToken` (grant_type refresh_token) toman de ahí el client_id y la URL del realm (`TokenPorAmbienteTest`).

| | SANDBOX | PRODUCCION |
|---|---|---|
| IdP | https://idp.comprobanteselectronicos.go.cr/auth | igual |
| Realm | rut-stag | rut |
| URL del token | …/auth/realms/rut-stag/protocol/openid-connect/token | …/auth/realms/rut/protocol/openid-connect/token |
| Client Id | api-stag | api-prod |
| API base | https://api-sandbox.comprobanteselectronicos.go.cr | https://api.comprobanteselectronicos.go.cr |
| Recepción (POST) y consulta (GET …/{clave}) | base + /recepcion/v1/recepcion | igual |
| Mensajes del receptor | base + /mensajes-receptor/v1/mensajes/{cédula} | igual |

Fuente: Anexo 3 (`docs/referencias/ANEXOS_Y_ESTRUCTURAS_V4.4/anexo-3-api-comprobantes.md`), l.25 (API de recepción de producción), l.39 (IdP) y l.60-62 (realm rut, Client Id api-prod).
- El Anexo no da el client_id ni el realm de pruebas. Para SANDBOX se conservan api-stag y rut-stag, con los que la aplicación ya emite y Hacienda acepta.
- Para la API de pruebas el Anexo menciona https://api.comprobanteselectronicos.go.cr/recepcion-sandbox/v1/ (l.31). Se conserva la URL base api-sandbox, la que está en uso y responde.

### Doble clic sobre la misma factura (review 1)
- `generarParaEmision` (y `generarDesdeFactura` cuando se llama directo, desde /generar o la reemisión) toma como **primera sentencia** de la etapa a el bloqueo de la fila de la factura (`FacturaRepository.findByIdParaActualizar`, `SELECT ... FOR UPDATE`).
- En MySQL (REPEATABLE READ) la vista de lectura nace con la primera lectura no bloqueante. Por eso el chequeo del comprobante vigente, que viene después, ve el que confirmó la emisión anterior.
- Si hay un vigente que no es RECHAZADO, la segunda emisión responde 409 «La factura ya tiene un comprobante en curso o emitido» (`EmisionNoPermitidaException`), antes de tomar el consecutivo.
- Orden de bloqueos: factura, luego configuracion_hacienda; es el mismo que la nota de crédito, que ya bloquea la factura (T-032).
- No se usa una lectura bloqueante del comprobante: sin filas que la cumplan, toma bloqueos de hueco que se cruzaban con el INSERT de otra factura (deadlock observado en las IT de concurrencia).
- Última defensa: si aun así chocara `uk_comprobante_un_vigente_por_factura`, `EnvioHaciendaServiceImpl.emitirFactura` lo traduce al mismo 409.

### app.scheduling.enabled=false
- Sin `@EnableScheduling` no existe el bean `taskScheduler`, así que el envío no programa el sondeo y lo deja en el log. La respuesta al usuario no cambia.
- El estado se resuelve después con «Consultar estado en Hacienda» o con el job horario, cuando vuelva a estar activo.
- Las pruebas corren con la propiedad en false (`src/test/resources/application.yml`) y llaman al sondeo y a los jobs de forma explícita.

## Recuperación automática del ENVIO_INCIERTO (T-045, ADR-049)
Antes de T-045 un ENVIO_INCIERTO (caída tras la marca previa al POST, timeout de lectura, o 404 de un ENVIADO) solo se resolvía con «Consultar estado en Hacienda». Ahora lo recupera el job horario `consultarEstadosPendientes`.

### Selección
- Vigentes en ENVIO_INCIERTO **del ambiente activo** (T-044; con la configuración ilegible, sin filtro como en T-041).
- Con el último envío (`fecha_envio`) de hace **15 minutos o más** (`MINUTOS_ANTES_DE_VERIFICAR`, la misma constante de la regla del 404).
- Con `consultas_error_hacienda` por debajo de `facturacion.hacienda.max-consultas-error` (5).
- Consulta propia en el repositorio (`findInciertosParaSondeo` y sus hermanas `...SinFiltroDeAmbiente` y `countInciertosParaSondeoDeOtroAmbiente`); ENVIADO, PROCESANDO y ERROR_HACIENDA conservan su ventana de 5 minutos (`findParaSondeo`). El log de excluidos por ambiente suma ambas.

### Interpretación
El job usa `consultarYActualizarEstado`, sin lógica propia:
- Hacienda lo tiene (200): se adopta su estado (recibido/procesando → PROCESANDO, aceptado → ACEPTADO, rechazado → RECHAZADO, error → ERROR_HACIENDA).
- 404 pasados 15 minutos (`interpretarNoExiste`): NO_TRANSMITIDO. El job de reintentos (cada 15 minutos) lo reenvía con **la misma clave y el mismo consecutivo**, dentro del límite de 5 intentos.
- Si el reenvío vuelve a clasificarse ENVIO_INCIERTO (4xx de «ya recibido», timeout), a los 15 minutos la consulta siguiente adopta el estado real. El ciclo 404 → reenvío → incierto queda acotado por los 5 intentos y no hay más de un POST por intento (el POST nunca se repite dentro de la misma petición).

### Tope de consultas
- Una consulta automática que deja el comprobante en ENVIO_INCIERTO (error de red o excepción, respuesta sin ind-estado clasificable, 404 antes de tiempo) suma 1 a `consultas_error_hacienda`. Lo hace el job con `registrarConsultaIncierta`, en una transacción propia (la de la consulta se revierte con la excepción). Pasar de ENVIADO/PROCESANDO/ERROR_HACIENDA a ENVIO_INCIERTO no cuenta.
- Al llegar al máximo, el job deja de consultarlo y lo dice en el log («... consultas automáticas sin resolver el ENVIO_INCIERTO; se deja de consultar»). Queda para la consulta manual o para revisarlo en ATV.
- Salir de ENVIO_INCIERTO (NO_TRANSMITIDO, ACEPTADO, RECHAZADO, PROCESANDO...) reinicia el contador a 0.
- La consulta manual «Consultar estado en Hacienda» reinicia el contador de un ENVIO_INCIERTO, igual que el de ERROR_HACIENDA (ADR-031 §4), y lo devuelve al sondeo. Su comportamiento no cambia en lo demás.
- No cuentan el sondeo inmediato de T-037 (que sigue igual) ni las consultas manuales.

## Fuera del alcance
Cola durable o 202 con consulta desde la pantalla (alternativa descartada). Cambios en la máquina de estados de ADR-030. Notificaciones `@Async` (P-090). InformacionReferencia de sustitutos (P-073).
