-- ============================================================
-- DATOS COMPLETOS: CANTONES Y DISTRITOS DE SAN JOSÉ
-- Provincia 1 - San José (Datos más completos)
-- ============================================================

-- CANTONES DE SAN JOSÉ (20 cantones)
INSERT INTO cat_canton_cr (provincia_codigo, codigo, nombre) VALUES
('1', '01', 'San José'),
('1', '02', 'Escazú'),
('1', '03', 'Desamparados'),
('1', '04', 'Puriscal'),
('1', '05', 'Tarrazú'),
('1', '06', 'Aserrí'),
('1', '07', 'Mora'),
('1', '08', 'Goicoechea'),
('1', '09', 'Santa Ana'),
('1', '10', 'Alajuelita'),
('1', '11', 'Vásquez de Coronado'),
('1', '12', 'Acosta'),
('1', '13', 'Tibás'),
('1', '14', 'Moravia'),
('1', '15', 'Montes de Oca'),
('1', '16', 'Turrubares'),
('1', '17', 'Dota'),
('1', '18', 'Curridabat'),
('1', '19', 'Pérez Zeledón'),
('1', '20', 'León Cortés Castro')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- DISTRITOS DEL CANTÓN CENTRAL (01 - San José)
INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
('1', '01', '01', 'Carmen'),
('1', '01', '02', 'Merced'),
('1', '01', '03', 'Hospital'),
('1', '01', '04', 'Catedral'),
('1', '01', '05', 'Zapote'),
('1', '01', '06', 'San Francisco de Dos Ríos'),
('1', '01', '07', 'Uruca'),
('1', '01', '08', 'Mata Redonda'),
('1', '01', '09', 'Pavas'),
('1', '01', '10', 'Hatillo'),
('1', '01', '11', 'San Sebastián')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- DISTRITOS DE ESCAZÚ (02)
INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
('1', '02', '01', 'Escazú'),
('1', '02', '02', 'San Antonio'),
('1', '02', '03', 'San Rafael')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- DISTRITOS DE DESAMPARADOS (03)
INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
('1', '03', '01', 'Desamparados'),
('1', '03', '02', 'San Miguel'),
('1', '03', '03', 'San Juan de Dios'),
('1', '03', '04', 'San Rafael Arriba'),
('1', '03', '05', 'San Antonio'),
('1', '03', '06', 'Frailes'),
('1', '03', '07', 'Patarrá'),
('1', '03', '08', 'San Cristóbal'),
('1', '03', '09', 'Rosario'),
('1', '03', '10', 'Damas'),
('1', '03', '11', 'San Rafael Abajo'),
('1', '03', '12', 'Gravilias'),
('1', '03', '13', 'Los Guido')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- DISTRITOS DE GOICOECHEA (08)
INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
('1', '08', '01', 'Guadalupe'),
('1', '08', '02', 'San Francisco'),
('1', '08', '03', 'Calle Blancos'),
('1', '08', '04', 'Mata de Plátano'),
('1', '08', '05', 'Ipís'),
('1', '08', '06', 'Rancho Redondo'),
('1', '08', '07', 'Purral')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- DISTRITOS DE SANTA ANA (09)
INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
('1', '09', '01', 'Santa Ana'),
('1', '09', '02', 'Salitral'),
('1', '09', '03', 'Pozos'),
('1', '09', '04', 'Uruca'),
('1', '09', '05', 'Piedades'),
('1', '09', '06', 'Brasil')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- DISTRITOS DE ALAJUELITA (10)
INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
('1', '10', '01', 'Alajuelita'),
('1', '10', '02', 'San Josecito'),
('1', '10', '03', 'San Antonio'),
('1', '10', '04', 'Concepción'),
('1', '10', '05', 'San Felipe')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- DISTRITOS DE CORONADO (11)
INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
('1', '11', '01', 'San Isidro'),
('1', '11', '02', 'San Rafael'),
('1', '11', '03', 'Dulce Nombre de Jesús'),
('1', '11', '04', 'Patalillo'),
('1', '11', '05', 'Cascajal')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- DISTRITOS DE TIBÁS (13)
INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
('1', '13', '01', 'San Juan'),
('1', '13', '02', 'Cinco Esquinas'),
('1', '13', '03', 'Anselmo Llorente'),
('1', '13', '04', 'León XIII'),
('1', '13', '05', 'Colima')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- DISTRITOS DE MORAVIA (14)
INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
('1', '14', '01', 'San Vicente'),
('1', '14', '02', 'San Jerónimo'),
('1', '14', '03', 'La Trinidad')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- DISTRITOS DE MONTES DE OCA (15)
INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
('1', '15', '01', 'San Pedro'),
('1', '15', '02', 'Sabanilla'),
('1', '15', '03', 'Mercedes'),
('1', '15', '04', 'San Rafael')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- DISTRITOS DE CURRIDABAT (18)
INSERT INTO cat_distrito_cr (provincia_codigo, canton_codigo, codigo, nombre) VALUES
('1', '18', '01', 'Curridabat'),
('1', '18', '02', 'Granadilla'),
('1', '18', '03', 'Sánchez'),
('1', '18', '04', 'Tirrases')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- Verificar datos insertados
SELECT 
    'San José' AS provincia,
    COUNT(DISTINCT c.codigo) AS total_cantones,
    COUNT(*) AS total_distritos
FROM cat_canton_cr c
LEFT JOIN cat_distrito_cr d ON c.provincia_codigo = d.provincia_codigo 
    AND c.codigo = d.canton_codigo
WHERE c.provincia_codigo = '1';
