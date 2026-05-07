## 🚀 PASOS PARA ACTIVAR FACTURACIÓN ELECTRÓNICA

### Paso 1: Descargar XSD
```powershell
cd D:\programacion\java\spring-boot\whats_orders_manager
# Ejecutar script de descarga XSD (ver Sección 1)
```

### Paso 2: Configurar Certificado
```bash
# 1. Crear carpeta certificados/
mkdir certificados

# 2. Copiar tu archivo.p12 a certificados/

# 3. Agregar a .env.local:
echo "HACIENDA_CERTIFICADO_PATH=certificados/empresa.p12" >> .env.local
echo "HACIENDA_CERTIFICADO_PIN=tu_pin" >> .env.local
```

### Paso 3: Configurar API
```bash
# Agregar a .env.local:
echo "HACIENDA_AMBIENTE=stag" >> .env.local
echo "HACIENDA_API_USERNAME=cpj-3-101-XXXXXX@stag.comprobanteselectronicos.go.cr" >> .env.local
echo "HACIENDA_API_PASSWORD=tu_password" >> .env.local
```

### Paso 4: Configurar en UI
1. Iniciar aplicación
2. Ir a: **Facturas → Configuración Hacienda**
3. Crear nueva configuración:
   - Empresa: Seleccionar
   - Ambiente: Sandbox (para pruebas)
   - Usuario API: (de .env)
   - Password API: (de .env)
   - Certificado Path: certificados/empresa.p12
   - PIN Certificado: (de .env)
   - **Activar configuración**

### Paso 5: Prueba
1. Ir a: **Facturas**
2. Crear factura de prueba
3. Guardar factura
4. Click en botón **"Enviar a Hacienda"**
5. Esperar respuesta (5-30 segundos)
6. Verificar estado en columna "Estado FE"

---

