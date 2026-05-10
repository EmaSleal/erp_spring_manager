# 📚 Documentación del Proyecto - ERP Spring Manager

**Última actualización:** 4 de enero de 2026  
**Estado del proyecto:** 🟢 En desarrollo activo

---

## 🚀 INICIO RÁPIDO

### Para Nuevos Desarrolladores:
1. **Primero:** Lee [reportes/ESTADO_PROYECTO.md](reportes/ESTADO_PROYECTO.md) para ver el progreso actual
2. **Segundo:** Revisa [arquitectura/ARQUITECTURA_PROYECTO.md](arquitectura/ARQUITECTURA_PROYECTO.md)
3. **Tercero:** Consulta las [guias/](guias/) para configuración inicial
4. **Cuarto:** Revisa el [INDICE.txt](INDICE.txt) para navegar por la documentación

### Para Stakeholders:
1. [Estado del Proyecto](reportes/ESTADO_PROYECTO.md) - **Estado actual y progreso**
2. [Próximos Pasos](reportes/PROXIMOS_PASOS.md) - **Planificación futura**
3. [Decisiones Aplicadas](reportes/DECISIONES_APLICADAS.md) - **Decisiones clave**
4. [Plan Maestro](planificacion/PLAN_MAESTRO.txt) - **Visión general**

---

## 📂 ESTRUCTURA DE DOCUMENTACIÓN

La documentación está organizada en las siguientes categorías:

### 📐 [arquitectura/](arquitectura/)
Documentación sobre la arquitectura y diseño del sistema
- **ARQUITECTURA_PROYECTO.md** - Visión general de la arquitectura
- **COMPONENTES.md** - Componentes principales del sistema
- **ESTRUCTURA_PROYECTO.md** - Estructura de carpetas y módulos

### 🗄️ [base de datos/](base%20de%20datos/)
Scripts SQL y documentación de base de datos
- Scripts de migración (MIGRATION_*.sql)
- Scripts de corrección (FIX_*.sql)
- Scripts de inicialización (INIT_*.sql)
- Procedimientos almacenados (SP_*.sql)
- Triggers y funciones

### ⚙️ [configuracion/](configuracion/)
Guías de configuración del sistema
- **CONFIGURACION_EMAIL.md** - Configuración del servicio de email
- **CONFIGURACION_ENV_LOGGING.md** - Variables de entorno y logging
- **RESUMEN_CONFIGURACION_ENV_LOGGING.md** - Resumen de configuración

### 🎨 [diseno/](diseno/)
Diseño visual y mockups
- Mockups y wireframes
- Guías de diseño UI/UX

### 📖 [guias/](guias/)
Manuales y guías de usuario
- **GUIA_INICIALIZACION_PREFERENCIAS.md** - Inicialización de preferencias
- **GUIA_LOGGING.md** - Uso del sistema de logging
- **MANUAL_USUARIO_PERMISOS.md** - Manual de gestión de permisos

### 🔐 [permisos/](permisos/)
Documentación del sistema de permisos y roles
- **MAPEO_SISTEMA_PERMISOS.md** - Mapeo del sistema de permisos
- **RESUMEN_ROLES_PERMISOS.md** - Resumen de roles y permisos

### 📋 [planificacion/](planificacion/)
Planificación y decisiones técnicas
- **PLAN_MAESTRO.txt** - Plan completo del proyecto
- **DECISIONES_TECNICAS.txt** - Decisiones técnicas aprobadas
- **RESUMEN_APROBACION.txt** - Resumen ejecutivo
- **MEJORAS_FUTURAS.md** - Mejoras planificadas
- [decisiones/](planificacion/decisiones/) - Decisiones por sprint

### 🔄 [refactorizacion/](refactorizacion/)
Documentación de refactorizaciones realizadas
- **CHECKLIST_REFACTORIZACION.md** - Lista de verificación
- **GUIA_REFACTORIZACION_MODULAR.md** - Guía de refactorización modular
- **INDICE_REFACTORIZACION.md** - Índice de documentos de refactorización
- **MAPEO_IMPORTS_REFACTORIZACION.md** - Mapeo de imports
- Documentación de refactorizaciones específicas

### 📚 [referencias/](referencias/)
Material de referencia y documentación externa
- **FUNCIONALIDADES_ERP.txt** - Funcionalidades tipo ERP
- [roadmap/](referencias/roadmap/) - Roadmap completo del proyecto

### 📊 [reportes/](reportes/)
Reportes de estado y decisiones
- **ESTADO_PROYECTO.md** - Estado actual del proyecto
- **DECISIONES_APLICADAS.md** - Decisiones técnicas aplicadas
- **PROXIMOS_PASOS.md** - Próximos pasos planificados
- **RESUMEN_IMPLEMENTACIONES.md** - Resumen de implementaciones
- Reportes de migraciones (REPORTE_MIGRACION_*.txt)

### 💻 [snippets/](snippets/)
Fragmentos de código y ejemplos
- Ejemplos de controladores
- Ejemplos de servicios
- Código reutilizable

### 🏃 [sprints/](sprints/)
Documentación organizada por sprints
- [SPRINT_1/](sprints/SPRINT_1/) - Sprint 1
- [SPRINT_2/](sprints/SPRINT_2/) - Sprint 2
- [SPRINT_3/](sprints/SPRINT_3/) - Sprint 3
- [SPRINT_4/](sprints/SPRINT_4/) - Sprint 4
- [fixes/](sprints/fixes/) - Correcciones y fixes

---

## 🔍 CÓMO ENCONTRAR INFORMACIÓN

### Preguntas Frecuentes

**¿Cuál es el estado actual del proyecto?**
→ [reportes/ESTADO_PROYECTO.md](reportes/ESTADO_PROYECTO.md)

**¿Cómo funciona la arquitectura del sistema?**
→ [arquitectura/ARQUITECTURA_PROYECTO.md](arquitectura/ARQUITECTURA_PROYECTO.md)

**¿Cómo configuro el sistema de email?**
→ [configuracion/CONFIGURACION_EMAIL.md](configuracion/CONFIGURACION_EMAIL.md)

**¿Cómo funciona el sistema de permisos?**
→ [permisos/RESUMEN_ROLES_PERMISOS.md](permisos/RESUMEN_ROLES_PERMISOS.md)

**¿Dónde están las migraciones de base de datos?**
→ [base de datos/](base%20de%20datos/)

**¿Cuáles son los próximos pasos del proyecto?**
→ [reportes/PROXIMOS_PASOS.md](reportes/PROXIMOS_PASOS.md)

**¿Qué decisiones técnicas se han tomado?**
→ [planificacion/DECISIONES_TECNICAS.txt](planificacion/DECISIONES_TECNICAS.txt)

**¿Dónde están las guías para usuarios?**
→ [guias/](guias/)

**¿Qué refactorizaciones se han realizado?**
→ [refactorizacion/INDICE_REFACTORIZACION.md](refactorizacion/INDICE_REFACTORIZACION.md)

---

## 📝 CONVENCIONES

### Formatos de Archivo
- **Markdown (.md)** - Documentos actuales, resúmenes, guías
- **Text (.txt)** - Documentos de planificación, checklists, índices
- **SQL (.sql)** - Scripts de base de datos

### Prefijos de Archivos
- **MIGRATION_** - Scripts de migración de base de datos
- **FIX_** - Scripts de corrección o documentos de fixes
- **GUIA_** - Documentación de usuario y guías
- **RESUMEN_** - Documentos consolidados y resúmenes
- **CHECKLIST_** - Listas de verificación
- **INDICE_** - Índices y tabla de contenidos
- **MAPEO_** - Mapeos y diagramas

### Símbolos
- ✅ = Completado
- ⏳ = En progreso
- ❌ = Pendiente o error
- ⭐ = Importante o destacado
- 📖 = Documentación
- 🔧 = Configuración
- 🐛 = Fix o corrección

---

## 📦 ARCHIVOS PRINCIPALES

En la raíz de `docs/` encontrarás:

- **README.md** (este archivo) - Guía principal de la documentación
- **INDICE.txt** - Índice completo de todos los documentos

---

## 🔄 ÚLTIMA REORGANIZACIÓN

**Fecha:** 4 de enero de 2026

**Cambios realizados:**
- ✅ Reorganización completa de la estructura de documentación
- ✅ Creación de carpetas temáticas (arquitectura, configuración, guías, etc.)
- ✅ Movimiento de archivos a sus categorías correspondientes
- ✅ Actualización del README principal con estructura clara
- ✅ Mejora en la navegabilidad de la documentación

**Estructura anterior:** Todos los archivos en la raíz de `docs/`  
**Estructura actual:** Archivos organizados por categorías en subcarpetas

---

## 📧 CONTACTO

Para más información sobre el proyecto, consulta los documentos de [planificacion/](planificacion/) o revisa el [reportes/ESTADO_PROYECTO.md](reportes/ESTADO_PROYECTO.md).

---

*Documentación del proyecto ERP Spring Manager*  
*Última actualización: 4 de enero de 2026*
