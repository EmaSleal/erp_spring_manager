# Envío de comprobantes por correo con Resend (T-055, ADR-070)

Procedimiento de despliegue y operación. Cuando Hacienda acepta una factura o una nota de crédito, el ERP registra una
entrega en la misma transacción y un job la envía por Resend al correo del receptor con tres adjuntos: el PDF, el XML
firmado y la respuesta de Hacienda. **El envío viene desactivado por defecto.** Los recordatorios de pago siguen apagados
(P-110) y la configuración SMTP existente (`configuracion_email`, ADR-027) no se toca.

## 1. Variables y propiedades

La clave solo sale de la variable de entorno `RESEND_API_KEY`; no se guarda en la base, no se imprime en el log ni en los
errores de las entregas. En `.env` va con su valor; en `.env.example` y `docker-compose.yml` se declara sin valor.

| Variable de entorno | Propiedad | Por defecto | Significado |
|---|---|---|---|
| `CORREO_COMPROBANTES_HABILITADO` | `correo.comprobantes.habilitado` | `false` | Interruptor general. Apagado, las entregas se registran `PENDIENTE` pero el job no envía ni consume intentos. |
| `RESEND_API_KEY` | `correo.resend.api-key` | vacío | Clave de la API de Resend. |
| `CORREO_REMITENTE` | `correo.remitente` | `Monrachem <no-reply@astro-solutions.net>` | Remitente; su dominio debe estar verificado en Resend (sección 2). |
| `CORREO_REPLY_TO` | `correo.reply-to` | vacío | Si está vacío, el cuerpo dice «Este correo no recibe respuestas». |
| `CORREO_CCO` | `correo.cco` | vacío | Copia oculta de cada envío (varias direcciones separadas por coma). |
| `CORREO_SANDBOX_DESTINATARIO_PRUEBA` | `correo.sandbox.destinatario-prueba` | vacío | Destino de los comprobantes del ambiente SANDBOX (sección 3). |
| `CORREO_TOPE_DIARIO` | `correo.tope-diario` | `100` | Máximo de envíos `ENVIADO` por día (zona America/Costa_Rica). |
| `RESEND_URL_BASE` | `correo.resend.url-base` | `https://api.resend.com` | Solo para pruebas con un servidor local. |
| (propiedad) | `correo.reintentos.maximo` | `5` | Intentos antes de pasar a `FALLIDO`. |
| (propiedad) | `correo.reintentos.espera-inicial` | `PT5M` | Espera tras el primer fallo; se duplica en cada intento (5, 10, 20, 40 min). |
| (propiedad) | `correo.resend.connect-timeout` / `read-timeout` | `5s` / `20s` | Tiempos máximos de conexión y lectura. |
| (propiedad) | `correo.job.lote` / `correo.job.reserva` / `correo.job.intervalo` | `20` / `PT10M` / `PT1M` | Entregas por corrida, reserva de la entrega que se está enviando e intervalo del job. |

El job respeta `APP_SCHEDULING_ENABLED`: con `false` no se ejecuta.

Si `correo.comprobantes.habilitado=true` y falta la clave o el remitente, la aplicación escribe un WARN al arrancar con lo que
falta (nunca el valor de la clave) y las entregas quedan `PENDIENTE` sin consumir intentos hasta que se corrija.

## 2. Verificar el dominio astro-solutions.net en Resend

Los datos de esta sección son los que Resend documenta para un dominio propio; confirmarlos en el panel de Resend
(Domains), que muestra los valores exactos de su dominio.

1. En Resend, **Domains > Add Domain** con `astro-solutions.net` (conviene la región más cercana a los destinatarios).
2. Resend muestra los registros que hay que crear en Cloudflare (DNS only, sin proxy):

   | Tipo | Nombre | Contenido | Para qué |
   |---|---|---|---|
   | TXT | `resend._domainkey` | clave pública DKIM que muestra Resend (`p=...`) | Firma DKIM |
   | MX | `send` | `feedback-smtp.<región>.amazonses.com`, prioridad 10 (valor exacto en el panel) | Retorno de rebotes (bounce) |
   | TXT | `send` | `v=spf1 include:amazonses.com ~all` (valor exacto en el panel) | SPF del dominio de retorno |

3. **SPF y el registro actual de SES/useSend.** El SPF de Resend va en el subdominio `send.astro-solutions.net`, que es el
   dominio del Return-Path. SPF se evalúa contra el dominio del Return-Path, no contra el `From`, así que **no hay que
   modificar el SPF del dominio raíz** que ya usa SES/useSend: los dos conviven porque están en nombres distintos
   (`astro-solutions.net` y `send.astro-solutions.net`). Tampoco se tocan los DKIM de SES.
4. Si en algún caso hiciera falta un solo SPF en el mismo nombre (por ejemplo, si se envía con Return-Path en el dominio
   raíz), se combinan los `include` en **un único** registro TXT; dos registros `v=spf1` en el mismo nombre invalidan SPF
   (error permanente):

   ```
   v=spf1 include:amazonses.com include:<include-que-indique-resend> ~all
   ```

   Cuidado con el límite de 10 consultas DNS de SPF: cada `include` suma.
5. **DMARC:** se conserva el registro `_dmarc.astro-solutions.net` existente. Con la alineación relajada (por defecto,
   `aspf=r; adkim=r`) basta con que DKIM firme con un dominio que sea el del `From` o un subdominio suyo: Resend firma con
   `d=astro-solutions.net` mediante la clave `resend._domainkey`, así que DKIM queda alineado. SPF se alinea de forma
   relajada porque el Return-Path `send.astro-solutions.net` es subdominio del `From`. Si el DMARC actual usa alineación
   estricta (`aspf=s`), SPF no alineará, pero DMARC pasa con solo DKIM alineado.
6. Esperar a que Resend marque el dominio como **Verified** (suele tardar de minutos a unas horas). Con el dominio sin
   verificar, la API responde 403 y las entregas pasan a `FALLIDO` (error definitivo).

## 3. Probar en producción antes de habilitar

Los comprobantes de SANDBOX nunca se envían al correo del receptor. Con `correo.sandbox.destinatario-prueba` configurado se
envían a esa dirección con el asunto prefijado `[SANDBOX]`; sin ella quedan `OMITIDO_SANDBOX`.

1. Declarar `RESEND_API_KEY` y `CORREO_SANDBOX_DESTINATARIO_PRUEBA=<buzón propio>` en el `.env` del servidor. Dejar
   `CORREO_COMPROBANTES_HABILITADO=false` y reiniciar la aplicación.
2. Con el ambiente SANDBOX activo, aceptar una factura de prueba. La entrega queda `PENDIENTE` (se ve en el detalle y en
   `/facturas/entregas?estado=PENDIENTE`).
3. Poner `CORREO_COMPROBANTES_HABILITADO=true` y reiniciar. **Primero solo con SANDBOX:** al habilitarse, el job envía
   también las entregas `PENDIENTE` acumuladas, así que antes hay que revisar `/facturas/entregas?estado=PENDIENTE`; las
   de PRODUCCION saldrían al correo real de cada cliente.
4. Verificar en el buzón de prueba: asunto `[SANDBOX] ...`, PDF, XML firmado y respuesta de Hacienda adjuntos, nombrados con
   la clave.
5. Con el resultado correcto, cambiar el ambiente a PRODUCCION. Las siguientes aceptaciones se envían al receptor, con la
   copia oculta si `CORREO_CCO` está definida.

## 4. Leer el historial y reenviar

- **Detalle de la factura** (modal) y **detalle de la nota de crédito**: bloque «Correo del comprobante» con destinatario,
  estado, intentos, fecha y error resumido de cada entrega, la más reciente primero.
- **Listado** `/facturas/entregas?estado=FALLIDO` o `?estado=PENDIENTE`, con enlace al documento.
- **Panel** `/facturas/inicio`: dos tarjetas, «Correos de comprobantes fallidos» y «pendientes vencidos» (los `PENDIENTE` cuyo
  próximo intento ya pasó: con el envío deshabilitado o sin clave, aparecen aquí).

| Estado | Significado |
|---|---|
| `PENDIENTE` | Espera su próximo intento (o a que se habilite el envío). |
| `ENVIADO` | Resend lo aceptó; se guarda su id. No garantiza la entrega al buzón. |
| `FALLIDO` | Se agotaron los intentos o Resend rechazó la petición (error definitivo). El error resumido dice por qué. |
| `OMITIDO_SIN_CORREO` | El receptor no tiene correo. Se corrige en el cliente y se reenvía a mano. |
| `OMITIDO_SANDBOX` | Comprobante de SANDBOX sin destinatario de prueba configurado. |

**Reenviar:** con el permiso `COMPROBANTE_REENVIAR_CORREO` (asignado a ADMIN) y el comprobante ACEPTADO, el botón
«Reenviar comprobante por correo» pide el destinatario (prellenado con el del receptor; se puede cambiar) y crea una entrega
MANUAL `PENDIENTE` que el job envía en el siguiente ciclo. Queda registrado el usuario y la actividad
`COMPROBANTE_REENVIO_CORREO`. La regla de SANDBOX también aplica al reenvío.

## 5. Tope diario y límite de Resend

- Se envían como máximo `correo.tope-diario` correos al día (100 por defecto, el cupo diario del plan gratuito de Resend). Al
  alcanzarlo, el resto de las entregas pasa a las 00:00 (hora de Costa Rica) del día siguiente, sin consumir intentos.
- Si Resend responde 429, el job se detiene, reprograma para el día siguiente lo pendiente y no consume intentos.
- 5xx y timeouts se reintentan con espera creciente hasta `correo.reintentos.maximo`; luego `FALLIDO`. Un 4xx de validación
  (por ejemplo, correo inválido o dominio no verificado) es `FALLIDO` de inmediato.

## 6. Recordatorios de pago

Siguen apagados (P-110): este cambio no los habilita ni usa su transporte. `EmailServiceImpl` y `configuracion_email` no se
modifican.

## 7. Desactivar o revertir

`CORREO_COMPROBANTES_HABILITADO=false` y reiniciar: el job deja de enviar; las aceptaciones siguen registrando entregas
`PENDIENTE`, que se enviarán al volver a habilitar.
