# 🐳 Guía de Despliegue con Docker

Este documento explica cómo ejecutar **ERP Orders Manager** usando Docker y Docker Compose.

---

## 📋 Prerrequisitos

- **Docker** 20.10+ instalado
- **Docker Compose** 2.0+ instalado
- Al menos **2GB de RAM** disponible
- Puertos **9090** libre en el host

---

## 🚀 Inicio Rápido

### 1. Construir y Levantar los Servicios

```powershell
# Construir las imágenes y levantar los contenedores
docker-compose up --build -d

# Ver los logs en tiempo real
docker-compose logs -f

# Ver logs solo de la aplicación
docker-compose logs -f app
```

### 2. Acceder a la Aplicación

Una vez levantados los servicios (toma ~60 segundos), accede a:

**URL:** http://localhost:9090

**Credenciales por defecto:**
- Usuario: `admin`
- Contraseña: (definida en datos iniciales)

---

## 🏗️ Arquitectura de Contenedores

```
┌─────────────────────────────────────┐
│  Host Machine (Windows)             │
│                                     │
│  Puerto 9090 → localhost:9090       │
└──────────────┬──────────────────────┘
               │
         ┌─────▼─────────────────────┐
         │  Docker Network           │
         │  (app-network)            │
         │                           │
         │  ┌──────────────────┐    │
         │  │  whats_orders_app│    │
         │  │  Puerto: 8080    │    │
         │  │  Expuesto: 9090  │    │
         │  └────────┬─────────┘    │
         │           │               │
         │           ▼               │
         │  ┌──────────────────┐    │
         │  │ whats_orders_mysql│   │
         │  │  Puerto: 3306     │   │
         │  │  (solo interno)   │   │
         │  └──────────────────┘    │
         └───────────────────────────┘
```

### Componentes:

1. **mysql** (MySQL 8.0)
   - Base de datos `facturas_monrachem`
   - Usuario: `root`
   - Contraseña: `password`
   - **NO expuesto al host** (solo accesible desde la app)
   - Volumen persistente: `mysql_data`

2. **app** (Spring Boot)
   - Puerto interno: `8080`
   - Puerto externo: `9090`
   - Conecta a MySQL vía red interna Docker
   - Logs persistentes en `./logs`

---

## 🔧 Comandos Útiles

### Gestión de Contenedores

```powershell
# Detener los servicios
docker-compose down

# Detener y eliminar volúmenes (⚠️ BORRA LA BD)
docker-compose down -v

# Reiniciar solo la aplicación
docker-compose restart app

# Ver estado de los servicios
docker-compose ps

# Ver uso de recursos
docker stats
```

### Ver Logs

```powershell
# Logs de todos los servicios
docker-compose logs -f

# Logs solo de MySQL
docker-compose logs -f mysql

# Últimas 100 líneas de logs de la app
docker-compose logs --tail=100 app
```

### Ejecutar Comandos en los Contenedores

```powershell
# Acceder a la shell de la aplicación
docker-compose exec app sh

# Acceder a MySQL CLI
docker-compose exec mysql mysql -u root -ppassword facturas_monrachem

# Ejecutar un script SQL
docker-compose exec -T mysql mysql -u root -ppassword facturas_monrachem < script.sql
```

---

## 🗄️ Inicialización de Base de Datos

Los scripts SQL en `docs/base de datos/` se ejecutan automáticamente al crear el contenedor MySQL por primera vez.

**Orden de ejecución:**
1. `CREATE_DB.txt` (si existe)
2. `EJECUTAR_DATOS_INICIALES.sql`
3. Demás archivos `.sql` en orden alfabético

**Para reinicializar la BD:**

```powershell
# 1. Detener y eliminar volumen
docker-compose down -v

# 2. Volver a levantar (recreará la BD)
docker-compose up -d
```

---

## ⚙️ Configuración Avanzada

### Variables de Entorno

Puedes crear un archivo `.env` en la raíz para personalizar la configuración:

```env
# Base de Datos
DB_PASSWORD=mi_password_seguro

# Email
EMAIL_USERNAME=tu_email@gmail.com
EMAIL_PASSWORD=tu_app_password

# WhatsApp
WHATSAPP_API_URL=https://graph.facebook.com/v17.0
WHATSAPP_PHONE_NUMBER_ID=123456789
WHATSAPP_ACCESS_TOKEN=tu_token

# Java
JAVA_OPTS=-Xmx2048m -Xms1024m
```

### Cambiar Puertos

Edita `docker-compose.yml`:

```yaml
services:
  app:
    ports:
      - "PUERTO_EXTERNO:8080"  # Ejemplo: "3000:8080"
```

### Configurar Memoria

Edita `docker-compose.yml`:

```yaml
services:
  app:
    environment:
      JAVA_OPTS: "-Xmx2048m -Xms1024m"  # 2GB máximo
```

---

## 🔍 Troubleshooting

### La aplicación no inicia

```powershell
# Ver logs completos
docker-compose logs app

# Verificar que MySQL esté saludable
docker-compose ps
```

### Error de conexión a MySQL

```powershell
# Verificar que MySQL esté listo
docker-compose exec mysql mysqladmin ping -h localhost -u root -ppassword

# Reiniciar servicios en orden
docker-compose down
docker-compose up -d mysql
# Esperar 30 segundos
docker-compose up -d app
```

### Puerto 9090 ya en uso

```powershell
# Ver qué proceso usa el puerto
netstat -ano | findstr :9090

# Opción 1: Detener el proceso
# Opción 2: Cambiar el puerto en docker-compose.yml
```

### Resetear TODO (⚠️ Elimina datos)

```powershell
# Detener servicios
docker-compose down

# Eliminar volúmenes
docker volume rm whats_orders_manager_mysql_data

# Eliminar imágenes
docker-compose down --rmi all

# Reconstruir desde cero
docker-compose up --build -d
```

---

## 🎯 Health Checks

Los servicios tienen health checks configurados:

### MySQL
- Comando: `mysqladmin ping`
- Intervalo: 10s
- Tiempo de inicio: 30s

### Aplicación Spring Boot
- Endpoint: `http://localhost:8080/actuator/health`
- Intervalo: 30s
- Tiempo de inicio: 60s

**Ver estado de salud:**

```powershell
docker-compose ps
```

Debe mostrar `healthy` en la columna Status.

---

## 📊 Respaldos

### Respaldar Base de Datos

```powershell
# Crear backup
docker-compose exec mysql mysqldump -u root -ppassword facturas_monrachem > backup_$(date +%Y%m%d).sql

# Restaurar backup
docker-compose exec -T mysql mysql -u root -ppassword facturas_monrachem < backup_20260105.sql
```

### Respaldar Volúmenes

```powershell
# Crear backup del volumen de MySQL
docker run --rm -v whats_orders_manager_mysql_data:/data -v ${PWD}:/backup ubuntu tar czf /backup/mysql_data_backup.tar.gz /data
```

---

## 🚀 Despliegue en Producción

### Recomendaciones:

1. **Cambiar credenciales**
   - Usa contraseñas fuertes
   - No uses `root` en producción
   - Usa variables de entorno o secretos

2. **Configurar HTTPS**
   - Agrega un proxy reverso (Nginx/Traefik)
   - Usa certificados SSL (Let's Encrypt)

3. **Limitar recursos**
   ```yaml
   services:
     app:
       deploy:
         resources:
           limits:
             cpus: '2'
             memory: 2G
   ```

4. **Backups automáticos**
   - Configura cron jobs para respaldos diarios
   - Almacena backups en ubicación externa

5. **Monitoreo**
   - Agrega Prometheus + Grafana
   - Configura alertas

---

## 📝 Notas Importantes

- ✅ MySQL **NO está expuesto** al host (solo accesible desde la app)
- ✅ Los datos de MySQL persisten en el volumen `mysql_data`
- ✅ La aplicación se expone en **puerto 9090** (internamente usa 8080)
- ✅ Los logs se guardan en `./logs` del host
- ⚠️ Las credenciales por defecto son para desarrollo, **cámbialas en producción**

---

## 🆘 Soporte

Para más información, consulta:
- [Documentación del proyecto](./docs/)
- [Sprint 4 Resumen](./docs/sprints/SPRINT_4/SPRINT_4_RESUMEN_FINAL.md)
- [Manual de Configuración](./docs/guias/MANUAL_CONFIGURACION_SISTEMA.md)

---

**Versión:** 1.0  
**Fecha:** 5 de enero de 2026  
**Proyecto:** ERP Orders Manager
