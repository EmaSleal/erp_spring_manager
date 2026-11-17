# Envío a Hacienda en etapas cortas (ADR-043)

Estado: aceptado el 2026-11-17. Resuelve P-076 (envío dentro del request) y P-089 (consecutivo sin bloqueo), y se suma P-088 (falso éxito al renovar el token). Complementa ADR-030 (mutabilidad fiscal y estados) y ADR-031.

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

## Fuera del alcance
Cola durable o 202 con consulta desde la pantalla (alternativa descartada). Cambios en la máquina de estados de ADR-030. Notificaciones `@Async` (P-090). InformacionReferencia de sustitutos (P-073).
