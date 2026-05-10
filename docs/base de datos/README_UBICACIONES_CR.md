# 📍 Catálogos de Ubicaciones - Costa Rica

Sistema de tablas auxiliares para almacenar códigos oficiales de **Provincia, Cantón, Distrito y Barrio** de Costa Rica según división territorial de Hacienda.

---

## 🎯 Objetivo

Facilitar la selección de ubicaciones en formularios de **Empresa** y **Cliente** para cumplir con los requisitos de **Facturación Electrónica Costa Rica v4.4**.

---

## 📦 Archivos Generados

### Scripts SQL

| Archivo | Descripción |
|---------|-------------|
| `CREATE_TABLAS_UBICACIONES_CR.sql` | Crea 4 tablas: `cat_provincia_cr`, `cat_canton_cr`, `cat_distrito_cr`, `cat_barrio_cr` + vista consolidada |
| `INSERT_PROVINCIAS_CR.sql` | Inserta las 7 provincias de Costa Rica |
| `INSERT_CANTONES_CR.sql` | Inserta cantones capitales (básico) |
| `INSERT_DISTRITOS_CR.sql` | Inserta distritos capitales (básico) |
| `INSERT_UBICACIONES_SAN_JOSE_COMPLETO.sql` | ✅ **Datos completos** de San José (20 cantones + distritos) |
| `EJECUTAR_UBICACIONES_CR.sql` | **Script maestro** - ejecuta todo en orden |

### Entidades JPA (Opcional)

| Archivo | Descripción |
|---------|-------------|
| `ProvinciaCostaRica.java` | Entidad para `cat_provincia_cr` |
| `CantonCostaRica.java` | Entidad para `cat_canton_cr` |
| `DistritoCostaRica.java` | Entidad para `cat_distrito_cr` |

---

## 🚀 Instalación

### Opción 1: Script Maestro (Recomendado)

```bash
# En MySQL Workbench o línea de comandos
cd docs/base\ de\ datos
mysql -u root -p nombre_bd < EJECUTAR_UBICACIONES_CR.sql
```

### Opción 2: Paso a Paso

```sql
-- 1. Crear tablas
SOURCE CREATE_TABLAS_UBICACIONES_CR.sql;

-- 2. Insertar provincias (7 registros)
SOURCE INSERT_PROVINCIAS_CR.sql;

-- 3. Insertar cantones básicos (7 registros)
SOURCE INSERT_CANTONES_CR.sql;

-- 4. Insertar distritos básicos (7 registros)
SOURCE INSERT_DISTRITOS_CR.sql;

-- 5. (OPCIONAL) Insertar datos completos de San José
SOURCE INSERT_UBICACIONES_SAN_JOSE_COMPLETO.sql;
```

---

## 📊 Estructura de Tablas

### `cat_provincia_cr`
```sql
+--------+------+------------+
| id     | código | nombre   |
+--------+------+------------+
| 1      | 1    | San José   |
| 2      | 2    | Alajuela   |
| ...    | ...  | ...        |
+--------+------+------------+
```

### `cat_canton_cr`
```sql
+----+----------------+--------+-------------+
| id | provincia_cod  | código | nombre      |
+----+----------------+--------+-------------+
| 1  | 1              | 01     | San José    |
| 2  | 1              | 02     | Escazú      |
| ...| ...            | ...    | ...         |
+----+----------------+--------+-------------+
```

### `cat_distrito_cr`
```sql
+----+-------+--------+--------+----------+
| id | prov  | canton | código | nombre   |
+----+-------+--------+--------+----------+
| 1  | 1     | 01     | 01     | Carmen   |
| 2  | 1     | 01     | 02     | Merced   |
| ...| ...   | ...    | ...    | ...      |
+----+-------+--------+--------+----------+
```

### Vista `cat_ubicacion_cr` (Consolidada)
```sql
SELECT * FROM cat_ubicacion_cr WHERE provincia_codigo = '1' LIMIT 5;

+------+----------+------+---------+------+---------+-------+---------+--------------+
| prov | prov_nom | cant | cant_nm | dist | dist_nm | barr  | barr_nm | cod_completo |
+------+----------+------+---------+------+---------+-------+---------+--------------+
| 1    | San José | 01   | San José| 01   | Carmen  | NULL  | NULL    | 1-01-01      |
| 1    | San José | 01   | San José| 02   | Merced  | NULL  | NULL    | 1-01-02      |
+------+----------+------+---------+------+---------+-------+---------+--------------+
```

---

## 🔍 Consultas Útiles

### Listar todos los cantones de una provincia
```sql
SELECT codigo, nombre 
FROM cat_canton_cr 
WHERE provincia_codigo = '1'  -- San José
ORDER BY codigo;
```

### Listar todos los distritos de un cantón
```sql
SELECT codigo, nombre 
FROM cat_distrito_cr 
WHERE provincia_codigo = '1' 
  AND canton_codigo = '01'  -- San José Central
ORDER BY codigo;
```

### Buscar ubicación completa por código
```sql
SELECT 
    provincia_nombre,
    canton_nombre,
    distrito_nombre
FROM cat_ubicacion_cr
WHERE provincia_codigo = '1'
  AND canton_codigo = '08'
  AND distrito_codigo = '01';
-- Resultado: San José > Goicoechea > Guadalupe
```

### Búsqueda por texto (LIKE)
```sql
SELECT * FROM cat_ubicacion_cr
WHERE canton_nombre LIKE '%Escazú%'
   OR distrito_nombre LIKE '%Escazú%';
```

---

## 🎨 Uso en Formularios

### Select Dinámico con JavaScript

```javascript
// Cargar cantones cuando se selecciona provincia
function cargarCantones(provinciaCodigo) {
    fetch(`/api/ubicaciones/cantones?provincia=${provinciaCodigo}`)
        .then(res => res.json())
        .then(cantones => {
            const select = document.getElementById('canton');
            select.innerHTML = '<option value="">Seleccione...</option>';
            cantones.forEach(c => {
                select.innerHTML += `<option value="${c.codigo}">${c.nombre}</option>`;
            });
        });
}

// Cargar distritos cuando se selecciona cantón
function cargarDistritos(provinciaCodigo, cantonCodigo) {
    fetch(`/api/ubicaciones/distritos?provincia=${provinciaCodigo}&canton=${cantonCodigo}`)
        .then(res => res.json())
        .then(distritos => {
            const select = document.getElementById('distrito');
            select.innerHTML = '<option value="">Seleccione...</option>';
            distritos.forEach(d => {
                select.innerHTML += `<option value="${d.codigo}">${d.nombre}</option>`;
            });
        });
}
```

### Endpoint REST (Ejemplo)

```java
@RestController
@RequestMapping("/api/ubicaciones")
public class UbicacionController {
    
    @GetMapping("/cantones")
    public List<CantonCostaRica> getCantones(@RequestParam String provincia) {
        return cantonRepository.findByProvinciaCodigoOrderByNombre(provincia);
    }
    
    @GetMapping("/distritos")
    public List<DistritoCostaRica> getDistritos(
        @RequestParam String provincia,
        @RequestParam String canton
    ) {
        return distritoRepository.findByProvinciaCodigoAndCantonCodigoOrderByNombre(
            provincia, canton
        );
    }
}
```

---

## 📝 Datos Incluidos

### ✅ Completo
- **7 Provincias**: Todas las provincias de Costa Rica
- **20 Cantones de San José**: Incluye todos (Central, Escazú, Desamparados, etc.)
- **Distritos principales de San José**: Carmen, Merced, Hospital, Zapote, Pavas, etc.

### ⚠️ Básico (Solo Capitales)
- **Cantones de otras provincias**: Solo cantón 01 (capital)
- **Distritos de otras provincias**: Solo distrito 01

---

## 🔗 Fuentes de Datos

Para datos completos de todas las provincias:

1. **PDF Oficial Hacienda CR:**
   - `docs/Codificacion,canton,provincia,distritoybarrio.pdf`

2. **INEC (Instituto Nacional de Estadística y Censos):**
   - https://www.inec.cr/
   - División Territorial Administrativa

3. **Ministerio de Hacienda CR:**
   - https://www.hacienda.go.cr/docs/catalogos/ubicaciones.xls
   - Anexos de Facturación Electrónica v4.4

---

## 🛠️ Agregar Más Datos

### Método 1: Insertar Manualmente

```sql
-- Ejemplo: Agregar cantones de Alajuela
INSERT INTO cat_canton_cr (provincia_codigo, codigo, nombre) VALUES
('2', '01', 'Alajuela'),
('2', '02', 'San Ramón'),
('2', '03', 'Grecia'),
('2', '04', 'San Mateo'),
('2', '05', 'Atenas')
-- ... continuar con el resto
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);
```

### Método 2: Importar desde CSV

```bash
# 1. Crear archivo cantones.csv
provincia_codigo,codigo,nombre
2,01,Alajuela
2,02,San Ramón
2,03,Grecia

# 2. Importar con MySQL
LOAD DATA LOCAL INFILE 'cantones.csv'
INTO TABLE cat_canton_cr
FIELDS TERMINATED BY ','
IGNORE 1 LINES
(provincia_codigo, codigo, nombre);
```

### Método 3: Extraer del PDF

Usa el script Python `extract_ubicaciones_from_pdf.py` y modifica la función `extract_from_pdf_manually()` para agregar más datos.

---

## ✅ Verificación

```sql
-- Contar registros
SELECT 
    (SELECT COUNT(*) FROM cat_provincia_cr) AS provincias,
    (SELECT COUNT(*) FROM cat_canton_cr) AS cantones,
    (SELECT COUNT(*) FROM cat_distrito_cr) AS distritos,
    (SELECT COUNT(*) FROM cat_barrio_cr) AS barrios;

-- Ver datos de San José
SELECT 
    c.nombre AS canton,
    COUNT(d.id) AS distritos
FROM cat_canton_cr c
LEFT JOIN cat_distrito_cr d ON c.provincia_codigo = d.provincia_codigo 
    AND c.codigo = d.canton_codigo
WHERE c.provincia_codigo = '1'
GROUP BY c.nombre
ORDER BY c.codigo;
```

---

## 🚨 Notas Importantes

1. **Integridad Referencial**: Las tablas usan `FOREIGN KEY` con `CASCADE DELETE`. Si borras una provincia, se borran sus cantones, distritos y barrios.

2. **No Sobrescribir Empresa/Cliente**: Las tablas `empresa` y `cliente` almacenan solo los **códigos** (ejemplo: provincia='1', canton='08', distrito='01'). Para mostrar nombres, hacer JOIN con estas tablas.

3. **Barrios Opcionales**: La tabla `cat_barrio_cr` está creada pero NO es obligatoria en facturación electrónica. Hacienda permite omitirla.

4. **Actualización Periódica**: La división territorial puede cambiar. Consultar INEC periódicamente.

---

## 📚 Recursos Adicionales

- **Checklist FE**: `docs/sprints/SPRINT_5/CHECKLIST_FACTURACION_ELECTRONICA.md`
- **Análisis Campos**: `docs/sprints/SPRINT_5/ANALISIS_DATOS_FALTANTES_FACTURACION_ELECTRONICA.md`
- **XSD v4.4**: https://tribunet.hacienda.go.cr/docs/esquemas/

---

**Autor:** Sistema ERP - WhatsApp Orders Manager  
**Fecha:** Marzo 2026  
**Versión:** 1.0
