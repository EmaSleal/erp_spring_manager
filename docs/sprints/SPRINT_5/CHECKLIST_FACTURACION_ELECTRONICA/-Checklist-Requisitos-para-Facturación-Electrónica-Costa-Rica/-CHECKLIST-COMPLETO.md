## 📋 CHECKLIST COMPLETO

### 1️⃣ Archivos XSD de Hacienda ⚠️ **CRÍTICO**

**Estado actual:** ❌ **FALTANTES** (solo existe README.md)

**¿Qué son los XSD?**
- Son esquemas XML que definen la estructura válida de los comprobantes electrónicos
- Hacienda los usa para validar que el XML cumpla con la especificación v4.4
- Sin ellos, el sistema puede generar XMLs inválidos que Hacienda rechazará

**Ubicación:** `src/main/resources/xsd/`

**Archivos requeridos:**
```
src/main/resources/xsd/
├── FacturaElectronica_V4.4.xsd        ❌ FALTA
├── TiqueteElectronico_V4.4.xsd        ❌ FALTA
├── NotaCreditoElectronica_V4.4.xsd    ❌ FALTA
└── NotaDebitoElectronica_V4.4.xsd     ❌ FALTA
```

**Cómo descargarlos:**

#### Opción A: PowerShell (Recomendado)
```powershell
# Ejecutar desde la raíz del proyecto
$baseUrl = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/"
$destDir = "src/main/resources/xsd/"

# Crear directorio si no existe
New-Item -ItemType Directory -Force -Path $destDir

$files = @(
    "FacturaElectronica_V4.4.xsd",
    "TiqueteElectronico_V4.4.xsd",
    "NotaCreditoElectronica_V4.4.xsd",
    "NotaDebitoElectronica_V4.4.xsd"
)

foreach ($file in $files) {
    $url = $baseUrl + $file
    $dest = $destDir + $file
    Write-Host "Descargando $file..." -ForegroundColor Cyan
    try {
        Invoke-WebRequest -Uri $url -OutFile $dest
        Write-Host "✅ $file descargado" -ForegroundColor Green
    } catch {
        Write-Host "❌ Error descargando $file" -ForegroundColor Red
    }
}

Write-Host "`n✅ Descarga completada!" -ForegroundColor Green
```

#### Opción B: Descarga Manual
1. Ir a: https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/
2. Descargar cada archivo XSD
3. Guardar en `src/main/resources/xsd/`

**Verificación:**
```java
// El sistema validará automáticamente usando XmlValidator.java
// Si falta el XSD, verás este WARNING en logs:
// "Esquema XSD no encontrado: xsd/FacturaElectronica_V4.4.xsd. 
//  Validando solo estructura básica."
```

**Impacto si faltan:**
- ⚠️ No se puede validar que el XML cumpla con especificación oficial
- ⚠️ Alto riesgo de rechazo por Hacienda
- ℹ️ El sistema funciona pero con validación básica (solo well-formed XML)

---

### 2️⃣ Certificado Digital .p12 🔐 **CRÍTICO**

**Estado actual:** ❓ **POR CONFIGURAR**

**¿Qué es el certificado .p12?**
- Es tu llave digital para firmar comprobantes electrónicos
- Emitido por una autoridad certificadora (ej: GBM, BCR, BCCR)
- Contiene tu identidad jurídica y clave privada

**Cómo obtenerlo:**
1. Contactar autoridad certificadora en Costa Rica:
   - Banco Central de Costa Rica (BCCR)
   - Banco de Costa Rica (BCR)
   - GBM Serfin
   - Camerfirma

2. Requisitos típicos:
   - Cédula jurídica de la empresa
   - Representante legal con cédula física
   - Personería jurídica vigente
   - Trámite puede tardar 3-7 días hábiles

3. Recibirás:
   - Archivo `.p12` o `.pfx`
   - PIN/contraseña del certificado
   - Validez: 1-2 años (renovación requerida)

**Configuración en el sistema:**

#### En `.env` o `.env.local`:
```properties
# Ruta al certificado (puede ser relativa o absoluta)
HACIENDA_CERTIFICADO_PATH=C:/certificados/empresa.p12
# O relativa al proyecto:
# HACIENDA_CERTIFICADO_PATH=certificados/empresa.p12

# PIN del certificado (¡NUNCA subir a git!)
HACIENDA_CERTIFICADO_PIN=tu_pin_super_secreto
```

#### En `application.properties`:
```properties
facturacion.electronica.certificado.ruta=${HACIENDA_CERTIFICADO_PATH}
facturacion.electronica.certificado.pin=${HACIENDA_CERTIFICADO_PIN}
```

**Ubicación recomendada del archivo:**
```
whats_orders_manager/
├── certificados/          ⬅️ CREAR ESTA CARPETA
│   └── empresa.p12       ⬅️ COLOCAR CERTIFICADO AQUÍ
└── .gitignore            ⬅️ ASEGURAR QUE IGNORE /certificados/
```

**⚠️ SEGURIDAD:**
```gitignore
# Agregar a .gitignore:
certificados/
*.p12
*.pfx
.env.local
```

**Verificación del certificado:**
El sistema tiene un método para validar el certificado:

```java
// En tu código de prueba:
CertificadoInfo info = firmaDigitalService.obtenerInfoCertificado(
    "ruta/al/certificado.p12", 
    "tu_pin"
);

System.out.println("Titular: " + info.titular());
System.out.println("Cédula: " + info.cedula());
System.out.println("Vigente: " + info.vigente());
System.out.println("Expira: " + info.fechaExpiracion());
```

**Impacto si falta:**
- ❌ **CRÍTICO**: No se puede firmar comprobantes
- ❌ Hacienda rechaza XML sin firma digital válida
- ❌ Sistema lanza excepción al intentar firmar

---

### 3️⃣ Credenciales API de Hacienda 🔑 **CRÍTICO**

**Estado actual:** ❓ **POR CONFIGURAR**

**¿Qué son las credenciales API?**
- Usuario y contraseña para acceder a la API de Hacienda
- Se obtienen del portal ATV (Administración Tributaria Virtual)
- Permiten enviar comprobantes y consultar respuestas

**Cómo obtenerlas:**

1. **Acceder al portal ATV de Hacienda:**
   - URL Producción: https://atv.hacienda.go.cr/
   - URL Sandbox/Pruebas: https://atvcalidadfe.hacienda.go.cr/

2. **Crear usuario de API:**
   - Iniciar sesión con representante legal
   - Ir a: Facturación Electrónica → Administración → Usuarios API
   - Crear nuevo usuario
   - Asignar permisos: "Emisión de Comprobantes"
   - Guardar usuario y contraseña generados

3. **Obtener NIT (Número Identificación Tributaria):**
   - Es la cédula jurídica de la empresa
   - Formato: 3-101-123456 (con guiones)

**Configuración en el sistema:**

#### En `.env.local`:
```properties
# Ambiente (stag = pruebas, prod = producción)
HACIENDA_AMBIENTE=stag

# Credenciales API
HACIENDA_API_USERNAME=cpj-3-101-123456@stag.comprobanteselectronicos.go.cr
HACIENDA_API_PASSWORD=tu_password_api

# URLs (ya configuradas en código, pero pueden sobrescribirse)
# Sandbox:
HACIENDA_API_URL=https://api.comprobanteselectronicos.go.cr/recepcion-sandbox/v1
HACIENDA_OAUTH_URL=https://idp.comprobanteselectronicos.go.cr/auth/realms/rut-sandbox

# Producción:
# HACIENDA_API_URL=https://api.comprobanteselectronicos.go.cr/recepcion/v1
# HACIENDA_OAUTH_URL=https://idp.comprobanteselectronicos.go.cr/auth/realms/rut
```

**Formato del username:**
- Sandbox: `cpj-{cedula_juridica}@stag.comprobanteselectronicos.go.cr`
- Producción: `cpj-{cedula_juridica}@prod.comprobanteselectronicos.go.cr`

**Impacto si faltan:**
- ❌ **CRÍTICO**: No se puede enviar comprobantes a Hacienda
- ❌ Fallarán todas las operaciones de API
- ❌ Sistema lanza 401 Unauthorized

---

### 4️⃣ Datos Maestros de la Empresa 📊 **IMPORTANTE**

**Estado actual:** ❓ **VERIFICAR**

**Datos requeridos en BD:**

```sql
-- Verificar en tabla 'empresa':
SELECT 
    cedula_juridica,    -- ej: '3-101-123456'
    nombre_comercial,   -- ej: 'Empresa SA'
    provincia,          -- ej: 'San José'
    canton,             -- ej: 'Central'
    distrito,           -- ej: 'Carmen'
    barrio,             -- ej: 'Los Yoses'
    otras_senas,        -- ej: 'Frente al parque'
    codigo_postal,      -- ej: '10101'
    telefono,           -- ej: '2222-3333'
    correo              -- ej: 'facturacion@empresa.com'
FROM empresa
WHERE id_empresa = 1;
```

**Campos críticos:**
- ✅ `cedula_juridica`: Debe coincidir con certificado
- ✅ `correo`: Para recibir notificaciones de Hacienda
- ✅ Dirección completa: Provincia, Cantón, Distrito (códigos oficiales)

**Códigos de ubicación:**
- Costa Rica usa códigos numéricos para provincia/cantón/distrito
- Referencia: https://www.hacienda.go.cr/docs/catalogos/ubicaciones.xls

---

### 5️⃣ Configuración de Base de Datos 🗄️ **COMPLETADO**

**Estado:** ✅ **CONFIGURADO**

**Tablas requeridas:**
- ✅ `configuracion_hacienda` - Configuración por empresa
- ✅ `comprobante_electronico` - Comprobantes generados
- ✅ `respuesta_hacienda` - Respuestas de Hacienda
- ✅ `consecutivo_hacienda` - Control de consecutivos
- ✅ `auditoria_configuracion_notificaciones` - Auditoría

**Stored Procedures:**
- ✅ `sp_listar_comprobantes_por_empresa`
- ✅ `sp_listar_comprobantes_por_estado`
- ✅ `sp_listar_comprobantes_por_fechas`
- ✅ `sp_obtener_estadisticas_comprobantes`
- ✅ `sp_listar_comprobantes_pendientes_reintento`
- ✅ `sp_buscar_comprobantes`

**Migraciones ejecutadas:**
- ✅ Sprint 2, 3, 4, 5 completados

---

### 6️⃣ Dependencias Maven 📦 **COMPLETADO**

**Estado:** ✅ **CONFIGURADO**

**Verificar en `pom.xml`:**
```xml
<!-- Firma Digital -->
<dependency>
    <groupId>org.apache.santuario</groupId>
    <artifactId>xmlsec</artifactId>
    <version>2.3.0</version>
</dependency>

<!-- HTTP Client para API -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- Cache para tokens -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

---

### 7️⃣ Configuración de Logging 📝 **RECOMENDADO**

**Agregar a `application.properties`:**
```properties
# Logging detallado para facturación electrónica
logging.level.api.astro.whats_orders_manager.modules.facturacion.electronica=DEBUG
logging.level.org.apache.xml.security=WARN
logging.level.org.springframework.web.reactive.function.client=DEBUG
```

---

### 8️⃣ Jobs Automáticos ⚙️ **COMPLETADO**

**Estado:** ✅ **IMPLEMENTADO**

**Jobs configurados:**
- ✅ `ComprobanteReintentosJob` - Reenvío automático cada 15 min
- ✅ `ConsultaEstadoJob` - Consulta estado cada hora
- ✅ `LimpiezaComprobantesJob` - Limpieza diaria

**Configuración:**
```properties
# Habilitar scheduling
spring.task.scheduling.enabled=true
```

---

