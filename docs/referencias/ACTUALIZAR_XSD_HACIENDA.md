# Actualizar los XSD de Hacienda

Procedimiento cuando el Ministerio de Hacienda publica una revisión de los esquemas de los comprobantes
electrónicos (hoy v4.4). Desde T-036 el XML firmado se valida contra estos XSD antes de cualquier llamada a
Hacienda; si un XSD queda desactualizado, se rechazan localmente comprobantes válidos (o se dejan pasar inválidos).

## 1. Origen oficial

Sitio del Ministerio de Hacienda: `https://www.hacienda.go.cr/docs/<Nombre>_V4.4.xsd`, con los cuatro tipos que
genera el sistema:

- `FacturaElectronica_V4.4.xsd`
- `TiqueteElectronico_V4.4.xsd`
- `NotaCreditoElectronica_V4.4.xsd`
- `NotaDebitoElectronica_V4.4.xsd`

`curl` recibe 403 de la CDN del sitio: descargarlos desde un navegador (o con WebFetch) y comprobar que el contenido
es el XML del esquema y no una página de error. Para una versión nueva, cambiar `V4.4` por la que corresponda y
revisar también el Anexo y estructuras de esa versión (`docs/referencias/`).

## 2. Dónde copiarlos

En `src/main/resources/xsd/`, con el mismo nombre. Ahí también está `xmldsig-core-schema.xsd` (firma XML): los XSD
oficiales lo importan con una ruta relativa que no existe en el classpath, y `XmlValidator` lo resuelve con la copia
local (sin red). Si la revisión cambia la firma, actualizarlo también. Detalles en
`src/main/resources/xsd/README.md`. Si cambia el nombre de archivo o la versión, ajustar las rutas de
`XmlValidator` (`XSD_PATH_*`).

## 3. Pruebas que hay que correr

1. Regresión de XSD de T-036: `ValidacionXsdAntesDeEnviarTest`, `XmlValidatorSchemaCacheTest`.
2. `NotaCreditoXmlTest` (XML firmado de factura, tiquete y nota de crédito contra el XSD real).
3. Suite completa y las IT fiscales:
   - `DB_URL=jdbc:mysql://127.0.0.1:1/none bash ./mvnw -o test`
   - `./mvnw verify` (`BloqueoFiscalFacturaIT`, `NotaCreditoIT`)

Si fallan, el XML que genera `XmlGeneratorServiceImpl` ya no cumple el esquema nuevo: corregir el generador, no el
XSD copiado. Ver el estado de los comprobantes en ERROR: el detalle del comprobante muestra los errores del validador
(línea, columna y mensaje).

## 4. Registro en la memoria del proyecto

- ratifact (`erp_spring_manager`): registrar la actualización como decisión (versión adoptada, fecha, fuente) o como
  pendiente si queda trabajo en el generador. Es la fuente de verdad.
- engram: solo observaciones técnicas (por ejemplo, cambios de comportamiento del validador o del acceso a la CDN).
