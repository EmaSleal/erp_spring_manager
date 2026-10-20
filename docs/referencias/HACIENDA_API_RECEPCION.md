# API de recepción de Hacienda: códigos que usa el sistema

Este es un resumen propio, no una copia, de la parte del API de comprobantes electrónicos que usa el cliente de Hacienda (`HaciendaApiServiceImpl`) y que decide los estados del comprobante (ADR-030).

Fuente oficial: <https://www.hacienda.go.cr/docs/ComprobantesElectronicosAPI.html> (consultada el 2026-10-20).

## POST /recepcion: enviar un comprobante

| Respuesta | Qué significa | Cómo lo clasifica el sistema |
|---|---|---|
| 2xx (la documentación indica **201**; en la práctica también se ve 202) | Hacienda recibió el documento. La validación queda pendiente y la respuesta llega después. El encabezado `Location` apunta a `/recepcion/{clave}`. | `RECIBIDO` → comprobante `ENVIADO` |
| **400** con `X-Error-Cause` | Error de validación. La documentación da un único ejemplo de causa: **el comprobante con esa clave ya fue recibido**. El texto es `El comprobante con la Clave [<clave>] ya fue recibido.` | Si la causa es la de «ya fue recibido» (reconocida de forma explícita, sin distinguir mayúsculas ni tildes, con o sin corchetes) → `ENVIO_INCIERTO`. Si coincide con un patrón de validación conocido → `NO_TRANSMITIDO`. Sin causa, o con una causa que no se puede clasificar → `ENVIO_INCIERTO` (regla prudente) |
| **401** | Token inválido o vencido. | `NO_TRANSMITIDO` (también el 403) |
| 5xx, timeout de lectura o conexión cortada | No está documentado. Hacienda pudo haber recibido el documento. | `ENVIO_INCIERTO` |
| Sin conexión (DNS, conexión rechazada, timeout de conexión) | El documento nunca salió. | `NO_TRANSMITIDO` |

La documentación no publica un catálogo de causas de `X-Error-Cause` para el 400, aparte del caso «ya fue recibido». Por eso `ClasificadorEnvioHacienda` solo declara `NO_TRANSMITIDO` cuando la causa coincide con un patrón de validación conocido (esquema, firma, campos requeridos y similares). Cualquier otra causa queda en `ENVIO_INCIERTO`.

## GET /recepcion/{clave}: consultar el estado

| Respuesta | Qué significa | Cómo lo usa el sistema |
|---|---|---|
| **200** con `ind-estado` | Estado del documento en Hacienda. | Ver la tabla siguiente |
| **404** con `X-Error-Cause` (`No existe el recurso con Clave = [<clave>]`) | Hacienda no tiene ningún documento con esa clave. | Señal explícita «no existe» (`documentoNoExiste`). Al verificar un `ENVIO_INCIERTO` → `NO_TRANSMITIDO`. En el sondeo, un `ENVIADO`, `PROCESANDO` o `ERROR_HACIENDA` → `ENVIO_INCIERTO`. Nunca se interpreta como «procesando» |
| **401** | Token inválido o vencido. | Error de consulta: el estado no cambia |

### Valores de `ind-estado`

La documentación enumera cinco valores, sin describirlos uno por uno:

| `ind-estado` | Estado del comprobante |
|---|---|
| `recibido` | `PROCESANDO` |
| `procesando` | `PROCESANDO` |
| `aceptado` | `ACEPTADO` |
| `rechazado` | `RECHAZADO` (es la única asignación de `RECHAZADO` en el sistema) |
| `error` | `ERROR_HACIENDA`: Hacienda tiene el documento, pero no lo resolvió. Queda bloqueado y el sondeo lo vuelve a consultar. Tras N consultas seguidas con `error` (`facturacion.hacienda.max-consultas-error`, 5 por defecto), el sondeo deja de consultarlo y el comprobante aparece señalado para revisarlo en ATV |
| cualquier otro, o ninguno | `ENVIO_INCIERTO`: no se sabe qué tiene Hacienda. Se resuelve con «verificar con Hacienda» |
