#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Extractor de Ubicaciones de Costa Rica desde PDF oficial de Hacienda
Genera scripts SQL para importar provincias, cantones, distritos y barrios.

Autor: Sistema de Facturación Electrónica
Fecha: 2024
"""

import re
from pathlib import Path

# Configuración de rutas
SCRIPT_DIR = Path(__file__).parent
PDF_FILE = SCRIPT_DIR.parent / "Codificacion,canton,provincia,distritoybarrio.pdf"  # El PDF está en docs/
TEXT_FILE = SCRIPT_DIR / "ubicaciones_cr.txt"
OUTPUT_DIR = SCRIPT_DIR

def extract_from_pdf():
    """Intenta extraer datos del PDF usando varias bibliotecas."""
    
    print("\n[1] Intentando extraer datos del PDF...")
    
    # Intentar con PyPDF2
    try:
        import PyPDF2
        data = extract_with_pypdf2()
        if data:
            return data
    except ImportError:
        print("  ⚠ PyPDF2 no está instalado")
    except Exception as e:
        print(f"  ⚠ Error con PyPDF2: {e}")
    
    # Intentar con pdfplumber
    try:
        import pdfplumber
        data = extract_with_pdfplumber()
        if data:
            return data
    except ImportError:
        print("  ⚠ pdfplumber no está instalado")
    except Exception as e:
        print(f"  ⚠ Error con pdfplumber: {e}")
    
    print("  ✗ No se pudo extraer del PDF")
    return None

def extract_with_pypdf2():
    """Extrae texto del PDF usando PyPDF2."""
    import PyPDF2
    
    if not PDF_FILE.exists():
        print(f"  ✗ PDF no encontrado: {PDF_FILE}")
        return None
    
    try:
        with open(PDF_FILE, 'rb') as file:
            reader = PyPDF2.PdfReader(file)
            text = ""
            for page in reader.pages:
                text += page.extract_text() + "\n"
        
        if text.strip():
            print(f"  ✓ Extraído con PyPDF2 ({len(text)} caracteres)")
            return parse_ubicaciones_text(text)
    except Exception as e:
        print(f"  ✗ Error: {e}")
    
    return None

def extract_with_pdfplumber():
    """Extrae texto del PDF usando pdfplumber (mejor calidad)."""
    import pdfplumber
    
    if not PDF_FILE.exists():
        print(f"  ✗ PDF no encontrado: {PDF_FILE}")
        return None
    
    try:
        with pdfplumber.open(PDF_FILE) as pdf:
            text = ""
            for page in pdf.pages:
                text += page.extract_text() + "\n"
        
        if text.strip():
            print(f"  ✓ Extraído con pdfplumber ({len(text)} caracteres)")
            return parse_ubicaciones_text(text)
    except Exception as e:
        print(f"  ✗ Error: {e}")
    
    return None

def extract_from_text_file():
    """Extrae datos de archivo de texto plano."""
    
    print("\n[2] Intentando leer archivo de texto...")
    
    if not TEXT_FILE.exists():
        print(f"  ✗ Archivo no encontrado: {TEXT_FILE}")
        print(f"  ℹ Para crear el archivo:")
        print(f"    1. Abre el PDF en Adobe Reader")
        print(f"    2. Copia todo el texto (Ctrl+A, Ctrl+C)")
        print(f"    3. Pega en: {TEXT_FILE}")
        return None
    
    try:
        with open(TEXT_FILE, 'r', encoding='utf-8') as f:
            text = f.read()
        
        if text.strip():
            print(f"  ✓ Leído archivo de texto ({len(text)} caracteres)")
            return parse_ubicaciones_text(text)
    except Exception as e:
        print(f"  ✗ Error: {e}")
    
    return None

def parse_ubicaciones_text(text):
    """Parsea el texto extraído y organiza por provincia/cantón/distrito/barrio."""
    
    # Patrón para formato tabular del PDF de Hacienda:
    # Columnas: Prov NombreProv Cant NombreCant Dist NombreDist Barr NombreBarr
    # Ejemplo: "1 San José 1 San José 1 CARMEN 1 Amón"
    pattern = r'^(\d+)\s+(.+?)\s+(\d+)\s+(.+?)\s+(\d+)\s+(.+?)(?:\s+(\d+)\s+(.+))?$'
    
    provincias = {}
    cantones = {}
    distritos = {}
    barrios = {}
    
    for line in text.split('\n'):
        line = line.strip()
        
        # Saltar líneas de encabezado
        if 'Provincia' in line or 'Nombre Provincia' in line:
            continue
            
        match = re.match(pattern, line)
        
        if match:
            prov = match.group(1)
            prov_nombre = match.group(2).strip()
            cant = match.group(3)
            cant_nombre = match.group(4).strip()
            dist = match.group(5)
            dist_nombre = match.group(6).strip()
            barr = match.group(7)  # Puede ser None
            barr_nombre = match.group(8) if barr else None
            
            # Formatear códigos a 2 dígitos (excepto provincia que es 1)
            cant_codigo = cant.zfill(2)
            dist_codigo = dist.zfill(2)
            
            # Registrar provincia
            if prov not in provincias or len(prov_nombre) > len(provincias.get(prov, '')):
                provincias[prov] = prov_nombre
            
            # Registrar cantón
            canton_key = (prov, cant_codigo)
            if canton_key not in cantones or len(cant_nombre) > len(cantones.get(canton_key, '')):
                cantones[canton_key] = cant_nombre
            
            # Registrar distrito
            distrito_key = (prov, cant_codigo, dist_codigo)
            if distrito_key not in distritos or len(dist_nombre) > len(distritos.get(distrito_key, '')):
                distritos[distrito_key] = dist_nombre
            
            # Registrar barrio si existe
            if barr and barr_nombre:
                barr_codigo = barr.zfill(2)
                barrio_key = (prov, cant_codigo, dist_codigo, barr_codigo)
                barr_nombre = barr_nombre.strip()
                if barrio_key not in barrios or len(barr_nombre) > len(barrios.get(barrio_key, '')):
                    barrios[barrio_key] = barr_nombre
    
    # Convertir a listas ordenadas
    result = {
        'provincias': [(k, v) for k, v in sorted(provincias.items())],
        'cantones': [(k[0], k[1], v) for k, v in sorted(cantones.items())],
        'distritos': [(k[0], k[1], k[2], v) for k, v in sorted(distritos.items())],
        'barrios': [(k[0], k[1], k[2], k[3], v) for k, v in sorted(barrios.items())]
    }
    
    print(f"  📊 Encontrados:")
    print(f"     - {len(result['provincias'])} provincias")
    print(f"     - {len(result['cantones'])} cantones")
    print(f"     - {len(result['distritos'])} distritos")
    print(f"     - {len(result['barrios'])} barrios")
    
    return result if result['provincias'] else None

def generate_sql_provincias(data=None):
    """Genera INSERT de provincias."""
    
    if data and 'provincias' in data and data['provincias']:
        # Usar datos extraídos
        provincias_list = data['provincias']
    else:
        # Datos de respaldo (7 provincias estándar)
        provincias_list = [
            ('1', 'San José'),
            ('2', 'Alajuela'),
            ('3', 'Cartago'),
            ('4', 'Heredia'),
            ('5', 'Guanacaste'),
            ('6', 'Puntarenas'),
            ('7', 'Limón')
        ]
    
    sql = """-- ============================================================
-- DATOS INICIALES: PROVINCIAS DE COSTA RICA
-- ============================================================
-- Fuente: Ministerio de Hacienda - División Territorial Administrativa
-- Generado automáticamente

INSERT INTO cat_provincia_cr (codigo, nombre) VALUES
"""
    
    values = []
    for codigo, nombre in provincias_list:
        nombre_escaped = nombre.replace("'", "''")
        values.append(f"('{codigo}', '{nombre_escaped}')")
    
    sql += ",\n".join(values) + "\nON DUPLICATE KEY UPDATE nombre = VALUES(nombre);\n"
    
    return sql

def generate_sql_cantones(data=None):
    """Genera INSERT de cantones."""
    
    if data and 'cantones' in data and data['cantones']:
        # Usar datos extraídos
        cantones_list = data['cantones']
    else:
        # Datos de respaldo (capitales provinciales)
        cantones_list = [
            ('1', '01', 'San José'),
            ('2', '01', 'Alajuela'),
            ('3', '01', 'Cartago'),
            ('4', '01', 'Heredia'),
            ('5', '01', 'Liberia'),
            ('6', '01', 'Puntarenas'),
            ('7', '01', 'Limón')
        ]
    
    sql = """-- ============================================================
-- DATOS INICIALES: CANTONES DE COSTA RICA
-- ============================================================
-- Fuente: Ministerio de Hacienda - División Territorial Administrativa
-- Generado automáticamente

INSERT INTO cat_canton_cr (provincia_codigo, codigo, nombre) VALUES
"""
    
    values = []
    for prov, codigo, nombre in cantones_list:
        nombre_escaped = nombre.replace("'", "''")
        values.append(f"('{prov}', '{codigo}', '{nombre_escaped}')")
    
    sql += ",\n".join(values) + "\nON DUPLICATE KEY UPDATE nombre = VALUES(nombre);\n"
    
    return sql

def generate_sql_distritos(data=None):
    """Genera INSERT de distritos."""
    
    if data and 'distritos' in data and data['distritos']:
        # Usar datos extraídos
        distritos_list = data['distritos']
    else:
        # Datos de respaldo (distritos capitales)
        distritos_list = [
            ('1', '01', '01', 'Carmen'),
            ('2', '01', '01', 'Alajuela'),
            ('3', '01', '01', 'Oriental'),
            ('4', '01', '01', 'Heredia'),
            ('5', '01', '01', 'Liberia'),
            ('6', '01', '01', 'Puntarenas'),
            ('7', '01', '01', 'Limón')
        ]
    
    sql = """-- ============================================================
-- DATOS INICIALES: DISTRITOS DE COSTA RICA
-- ============================================================
-- Fuente: Ministerio de Hacienda - División Territorial Administrativa
-- Generado automáticamente

INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
"""
    
    values = []
    for prov, canton, codigo, nombre in distritos_list:
        nombre_escaped = nombre.replace("'", "''")
        values.append(f"('{prov}', '{canton}', '{codigo}', '{nombre_escaped}')")
    
    sql += ",\n".join(values) + "\nON DUPLICATE KEY UPDATE nombre = VALUES(nombre);\n"
    
    return sql

def generate_sql_barrios(data=None):
    """Genera INSERT de barrios (solo si hay datos disponibles)."""
    
    if not data or 'barrios' not in data or not data['barrios']:
        return None  # No generar archivo si no hay datos
    
    barrios_list = data['barrios']
    
    sql = """-- ============================================================
-- DATOS INICIALES: BARRIOS DE COSTA RICA
-- ============================================================
-- Fuente: Ministerio de Hacienda - División Territorial Administrativa
-- Generado automáticamente
-- NOTA: Los barrios son opcionales según normativa de Hacienda

INSERT INTO cat_barrio_cr (provincia_codigo, canton_codigo, distrito_codigo, codigo, nombre) VALUES
"""
    
    values = []
    for prov, canton, distrito, codigo, nombre in barrios_list:
        nombre_escaped = nombre.replace("'", "''")
        values.append(f"('{prov}', '{canton}', '{distrito}', '{codigo}', '{nombre_escaped}')")
    
    sql += ",\n".join(values) + "\nON DUPLICATE KEY UPDATE nombre = VALUES(nombre);\n"
    
    return sql

def generate_sql_create_tables():
    """Genera script de creación de tablas."""
    
    sql = """-- ============================================================
-- TABLAS DE CATÁLOGOS: UBICACIONES DE COSTA RICA
-- ============================================================
-- Estructura para división territorial según Ministerio de Hacienda
-- Requerido para cumplimiento Facturación Electrónica v4.4

-- Tabla: Provincias
CREATE TABLE IF NOT EXISTS cat_provincia_cr (
    codigo CHAR(1) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_provincia_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Catálogo de 7 provincias de Costa Rica';

-- Tabla: Cantones
CREATE TABLE IF NOT EXISTS cat_canton_cr (
    provincia_codigo CHAR(1) NOT NULL,
    codigo CHAR(2) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (provincia_codigo, codigo),
    FOREIGN KEY (provincia_codigo) REFERENCES cat_provincia_cr(codigo) ON DELETE CASCADE,
    INDEX idx_canton_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Catálogo de cantones de Costa Rica (82 cantones)';

-- Tabla: Distritos
CREATE TABLE IF NOT EXISTS cat_distrito_cr (
    provincia_codigo CHAR(1) NOT NULL,
    canton_codigo CHAR(2) NOT NULL,
    codigo CHAR(2) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (provincia_codigo, canton_codigo, codigo),
    FOREIGN KEY (provincia_codigo, canton_codigo) 
        REFERENCES cat_canton_cr(provincia_codigo, codigo) ON DELETE CASCADE,
    INDEX idx_distrito_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Catálogo de distritos de Costa Rica (~487 distritos)';

-- Tabla: Barrios (Opcional)
CREATE TABLE IF NOT EXISTS cat_barrio_cr (
    provincia_codigo CHAR(1) NOT NULL,
    canton_codigo CHAR(2) NOT NULL,
    distrito_codigo CHAR(2) NOT NULL,
    codigo CHAR(2) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (provincia_codigo, canton_codigo, distrito_codigo, codigo),
    FOREIGN KEY (provincia_codigo, canton_codigo, distrito_codigo) 
        REFERENCES cat_distrito_cr(provincia_codigo, canton_codigo, codigo) ON DELETE CASCADE,
    INDEX idx_barrio_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Catálogo de barrios de Costa Rica (opcional según Hacienda)';

-- Vista: Ubicación completa
CREATE OR REPLACE VIEW cat_ubicacion_cr AS
SELECT 
    p.codigo AS provincia_codigo,
    p.nombre AS provincia_nombre,
    c.codigo AS canton_codigo,
    c.nombre AS canton_nombre,
    d.codigo AS distrito_codigo,
    d.nombre AS distrito_nombre,
    CONCAT(p.codigo, '-', c.codigo, '-', d.codigo) AS codigo_completo,
    CONCAT(p.nombre, ', ', c.nombre, ', ', d.nombre) AS ubicacion_completa
FROM cat_provincia_cr p
LEFT JOIN cat_canton_cr c ON p.codigo = c.provincia_codigo
LEFT JOIN cat_distrito_cr d ON c.provincia_codigo = d.provincia_codigo 
    AND c.codigo = d.canton_codigo
ORDER BY p.codigo, c.codigo, d.codigo;

-- Verificación
SELECT 'Tablas de ubicaciones CR creadas exitosamente' AS status;
"""
    
    return sql

def generate_master_sql():
    """Genera script maestro de ejecución."""
    
    sql = """-- ============================================================
-- SCRIPT MAESTRO: EJECUTAR TODOS LOS CATÁLOGOS DE UBICACIONES
-- ============================================================
-- Ejecuta todos los scripts en el orden correcto

-- 1. Crear tablas
SOURCE CREATE_TABLAS_UBICACIONES_CR.sql;

-- 2. Insertar provincias
SOURCE INSERT_PROVINCIAS_CR.sql;

-- 3. Insertar cantones
SOURCE INSERT_CANTONES_CR.sql;

-- 4. Insertar distritos
SOURCE INSERT_DISTRITOS_CR.sql;

-- 5. Insertar barrios (si existe)
-- SOURCE INSERT_BARRIOS_CR.sql;

-- Verificación final
SELECT 
    (SELECT COUNT(*) FROM cat_provincia_cr) AS total_provincias,
    (SELECT COUNT(*) FROM cat_canton_cr) AS total_cantones,
    (SELECT COUNT(*) FROM cat_distrito_cr) AS total_distritos,
    (SELECT COUNT(*) FROM cat_barrio_cr) AS total_barrios;

SELECT 'Catálogos de ubicaciones CR cargados exitosamente' AS status;
"""
    
    return sql

def main():
    """Función principal."""
    
    print("=" * 70)
    print("  GENERADOR DE CATÁLOGOS DE UBICACIONES - COSTA RICA")
    print("=" * 70)
    print("\nEste script genera los archivos SQL necesarios para:")
    print("  • cat_provincia_cr (7 provincias)")
    print("  • cat_canton_cr (~82 cantones)")
    print("  • cat_distrito_cr (~487 distritos)")
    print("  • cat_barrio_cr (barrios - opcional)")
    print("\n" + "=" * 70)
    
    # Intentar extraer datos
    data = extract_from_pdf()
    
    if not data:
        data = extract_from_text_file()
    
    if not data:
        print("\n" + "=" * 70)
        print("  ⚠ MODO BÁSICO: Usando datos de respaldo")
        print("=" * 70)
        print("\n  Se generarán scripts con datos básicos (capitales).")
        print("  Para obtener TODOS los datos:")
        print("    1. Instala biblioteca: pip install pdfplumber")
        print("    2. Ejecuta este script nuevamente")
        print("\n" + "=" * 70)
    else:
        print("\n" + "=" * 70)
        print("  ✓ EXTRACCIÓN EXITOSA")
        print("=" * 70)
    
    # Generar archivos SQL
    print("\nGenerando archivos SQL...")
    
    # CREATE TABLES
    output_file = OUTPUT_DIR / "CREATE_TABLAS_UBICACIONES_CR.sql"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(generate_sql_create_tables())
    print(f"  ✓ {output_file.name}")
    
    # INSERT PROVINCIAS
    output_file = OUTPUT_DIR / "INSERT_PROVINCIAS_CR.sql"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(generate_sql_provincias(data))
    print(f"  ✓ {output_file.name}")
    
    # INSERT CANTONES
    output_file = OUTPUT_DIR / "INSERT_CANTONES_CR.sql"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(generate_sql_cantones(data))
    print(f"  ✓ {output_file.name}")
    
    # INSERT DISTRITOS
    output_file = OUTPUT_DIR / "INSERT_DISTRITOS_CR.sql"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(generate_sql_distritos(data))
    print(f"  ✓ {output_file.name}")
    
    # INSERT BARRIOS (solo si hay datos)
    barrios_sql = generate_sql_barrios(data)
    if barrios_sql:
        output_file = OUTPUT_DIR / "INSERT_BARRIOS_CR.sql"
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(barrios_sql)
        print(f"  ✓ {output_file.name}")
    else:
        print(f"  ⊘ INSERT_BARRIOS_CR.sql (sin datos)")
    
    # MASTER SCRIPT
    output_file = OUTPUT_DIR / "EJECUTAR_UBICACIONES_CR.sql"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write(generate_master_sql())
    print(f"  ✓ {output_file.name}")
    
    # Estadísticas finales
    print("\n" + "=" * 70)
    print("  RESUMEN DE GENERACIÓN")
    print("=" * 70)
    
    if data:
        print(f"\n  Registros generados:")
        print(f"    • {len(data['provincias'])} provincias")
        print(f"    • {len(data['cantones'])} cantones")
        print(f"    • {len(data['distritos'])} distritos")
        if data['barrios']:
            print(f"    • {len(data['barrios'])} barrios")
    else:
        print("\n  Registros generados (MODO BÁSICO):")
        print(f"    • 7 provincias (completo)")
        print(f"    • 7 cantones (solo capitales)")
        print(f"    • 7 distritos (solo capitales)")
        print(f"    • 0 barrios")
    
    print("\n" + "=" * 70)
    print("  PRÓXIMOS PASOS")
    print("=" * 70)
    print("\n  1. Ejecutar en MySQL:")
    print(f"       SOURCE {OUTPUT_DIR / 'EJECUTAR_UBICACIONES_CR.sql'};")
    print("\n  2. Verificar carga:")
    print("       SELECT * FROM cat_provincia_cr;")
    print("       SELECT COUNT(*) FROM cat_canton_cr;")
    print("\n" + "=" * 70)
    print("\n  ✓ Proceso completado exitosamente")
    print("\n" + "=" * 70)

if __name__ == "__main__":
    main()
