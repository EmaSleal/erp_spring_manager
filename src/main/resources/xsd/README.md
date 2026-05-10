# Esquemas XSD de Hacienda Costa Rica v4.4

## 📥 Descarga de Esquemas

Los esquemas XSD oficiales deben descargarse desde el sitio del Ministerio de Hacienda de Costa Rica:

**URL Base:** `https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/`

## 📁 Archivos Requeridos

Descargar los siguientes archivos XSD y colocarlos en este directorio:

### 1. Factura Electrónica
- **Archivo:** `FacturaElectronica_V4.4.xsd`
- **URL:** https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/FacturaElectronica_V4.4.xsd
- **Uso:** Validación de facturas electrónicas estándar

### 2. Tiquete Electrónico
- **Archivo:** `TiqueteElectronico_V4.4.xsd`
- **URL:** https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/TiqueteElectronico_V4.4.xsd
- **Uso:** Validación de tiquetes electrónicos simplificados

### 3. Nota de Crédito Electrónica
- **Archivo:** `NotaCreditoElectronica_V4.4.xsd`
- **URL:** https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/NotaCreditoElectronica_V4.4.xsd
- **Uso:** Validación de notas de crédito

### 4. Nota de Débito Electrónica
- **Archivo:** `NotaDebitoElectronica_V4.4.xsd`
- **URL:** https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/NotaDebitoElectronica_V4.4.xsd
- **Uso:** Validación de notas de débito

### 5. Esquemas Auxiliares (Opcionales)
- `xmldsig-core-schema_V1.1.xsd` - Firma digital XML
- `XAdES01903v132-201601_V1.2.xsd` - XAdES para firma digital

## 🔧 Instrucciones de Descarga

### Opción 1: Descarga Manual
1. Acceder a cada URL listada arriba
2. Guardar el contenido XML en un archivo con el nombre indicado
3. Colocar todos los archivos en este directorio (`src/main/resources/xsd/`)

### Opción 2: Script PowerShell
```powershell
# Ejecutar desde el directorio raíz del proyecto
$baseUrl = "https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/"
$destDir = "src/main/resources/xsd/"

$files = @(
    "FacturaElectronica_V4.4.xsd",
    "TiqueteElectronico_V4.4.xsd",
    "NotaCreditoElectronica_V4.4.xsd",
    "NotaDebitoElectronica_V4.4.xsd"
)

foreach ($file in $files) {
    $url = $baseUrl + $file
    $dest = $destDir + $file
    Write-Host "Descargando $file..."
    Invoke-WebRequest -Uri $url -OutFile $dest
}

Write-Host "Descarga completada!"
```

### Opción 3: curl (Linux/Mac/Git Bash)
```bash
cd src/main/resources/xsd/
curl -O https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/FacturaElectronica_V4.4.xsd
curl -O https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/TiqueteElectronico_V4.4.xsd
curl -O https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/NotaCreditoElectronica_V4.4.xsd
curl -O https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/NotaDebitoElectronica_V4.4.xsd
```

## ✅ Verificación

Después de descargar, verificar que los archivos existan:

```
src/main/resources/xsd/
├── FacturaElectronica_V4.4.xsd
├── TiqueteElectronico_V4.4.xsd
├── NotaCreditoElectronica_V4.4.xsd
└── NotaDebitoElectronica_V4.4.xsd
```

## 📚 Documentación Oficial

- **Portal Hacienda:** https://www.hacienda.go.cr/
- **Documentación FE:** https://tribunet.hacienda.go.cr/docs/esquemas/
- **Especificación v4.4:** Anexos y resoluciones del Ministerio de Hacienda

## 🔍 Uso en el Código

Los XSD se usan en `XmlValidator.java` para validar que el XML generado cumple con la especificación:

```java
Schema schema = factory.newSchema(
    new File("src/main/resources/xsd/FacturaElectronica_V4.4.xsd")
);
Validator validator = schema.newValidator();
validator.validate(new StreamSource(new StringReader(xml)));
```

## 📌 Notas Importantes

1. **Versión:** Siempre usar v4.4 (versión vigente desde 2019)
2. **Actualización:** Verificar periódicamente si hay nuevas versiones
3. **Validación:** Los XSD son OBLIGATORIOS antes de enviar a Hacienda
4. **Encoding:** Los XSD están en UTF-8
5. **Namespace:** Verificar que el namespace en el XML coincida con el XSD

## 🚨 Troubleshooting

**Error: Cannot find schema**
- Verificar que los archivos XSD estén en el directorio correcto
- Verificar permisos de lectura del directorio

**Error: Schema parsing failed**
- Verificar integridad del archivo descargado
- Re-descargar el XSD desde la fuente oficial

**Error: Namespace mismatch**
- Verificar que el namespace en el XML sea exactamente:
  `https://cdn.comprobanteselectronicos.go.cr/xml-schemas/v4.4/facturaElectronica`
