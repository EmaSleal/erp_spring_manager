## 📊 INVENTARIO DE PERMISOS

### Permisos por Categoría

#### 1️⃣ FACTURACIÓN (7 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `FACTURA_VER` | Ver facturas | Visualizar listado y detalle de facturas | ❌ |
| `FACTURA_CREAR` | Crear facturas | Generar nuevas facturas de venta | ❌ |
| `FACTURA_EDITAR` | Editar facturas | Modificar facturas existentes (solo PENDIENTE) | ❌ |
| `FACTURA_ELIMINAR` | Eliminar facturas | Eliminar facturas del sistema | ✅ |
| `FACTURA_ANULAR` | Anular facturas | Anular facturas pagadas/completadas | ✅ |
| `FACTURA_EXPORTAR` | Exportar facturas | Exportar facturas a PDF/Excel | ❌ |
| `FACTURA_ENVIAR_EMAIL` | Enviar facturas | Enviar facturas por email a clientes | ❌ |

#### 2️⃣ CLIENTES (5 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `CLIENTE_VER` | Ver clientes | Visualizar listado y detalle de clientes | ❌ |
| `CLIENTE_CREAR` | Crear clientes | Registrar nuevos clientes | ❌ |
| `CLIENTE_EDITAR` | Editar clientes | Modificar información de clientes | ❌ |
| `CLIENTE_ELIMINAR` | Eliminar clientes | Eliminar clientes del sistema | ❌ |
| `CLIENTE_EXPORTAR` | Exportar clientes | Exportar listado de clientes | ❌ |

#### 3️⃣ PRODUCTOS (6 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `PRODUCTO_VER` | Ver productos | Visualizar catálogo de productos | ❌ |
| `PRODUCTO_CREAR` | Crear productos | Agregar nuevos productos al catálogo | ❌ |
| `PRODUCTO_EDITAR` | Editar productos | Modificar información de productos | ❌ |
| `PRODUCTO_ELIMINAR` | Eliminar productos | Eliminar productos del catálogo | ❌ |
| `PRODUCTO_AJUSTAR_INVENTARIO` | Ajustar inventario | Modificar cantidades en stock | ❌ |
| `PRODUCTO_EXPORTAR` | Exportar productos | Exportar catálogo de productos | ❌ |

#### 4️⃣ REPORTES (7 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `REPORTE_VENTAS` | Reporte de ventas | Ver reporte de ventas y gráficas | ❌ |
| `REPORTE_PRODUCTOS` | Reporte de productos | Ver reporte de productos más vendidos | ❌ |
| `REPORTE_CLIENTES` | Reporte de clientes | Ver reporte de clientes frecuentes | ❌ |
| `REPORTE_DASHBOARD` | Dashboard | Acceso al dashboard principal | ❌ |
| `REPORTE_EXPORTAR_PDF` | Exportar PDF | Exportar reportes a PDF | ❌ |
| `REPORTE_EXPORTAR_EXCEL` | Exportar Excel | Exportar reportes a Excel | ❌ |
| `REPORTE_EXPORTAR_CSV` | Exportar CSV | Exportar reportes a CSV | ❌ |

#### 5️⃣ CONFIGURACIÓN (5 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `CONFIG_VER` | Ver configuración | Visualizar configuración del sistema | ❌ |
| `CONFIG_EDITAR_EMPRESA` | Editar empresa | Modificar datos de la empresa | ✅ |
| `CONFIG_EDITAR_FACTURACION` | Editar facturación | Modificar configuración de facturación | ❌ |
| `CONFIG_EDITAR_EMAIL` | Editar email | Modificar configuración de email | ❌ |
| `CONFIG_EDITAR_WHATSAPP` | Editar WhatsApp | Modificar configuración de WhatsApp | ❌ |

#### 6️⃣ NOTIFICACIONES (5 permisos)
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `NOTIFICACION_VER` | Ver notificaciones | Ver notificaciones del sistema | ❌ |
| `NOTIFICACION_CREAR` | Crear notificaciones | Enviar notificaciones manuales | ❌ |
| `NOTIFICACION_MARCAR_LEIDA` | Marcar leída | Marcar notificaciones como leídas | ❌ |
| `NOTIFICACION_ELIMINAR` | Eliminar notificaciones | Eliminar notificaciones | ❌ |
| `NOTIFICACION_CONFIGURAR` | Configurar notificaciones | Gestionar preferencias de notificación | ❌ |

#### 7️⃣ USUARIOS (8 permisos) - SOLO ADMIN
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `USUARIO_VER` | Ver usuarios | Visualizar listado de usuarios | ✅ |
| `USUARIO_CREAR` | Crear usuarios | Registrar nuevos usuarios | ✅ |
| `USUARIO_EDITAR` | Editar usuarios | Modificar información de usuarios | ✅ |
| `USUARIO_ELIMINAR` | Eliminar usuarios | Eliminar usuarios del sistema | ✅ |
| `USUARIO_BLOQUEAR` | Bloquear usuarios | Bloquear/desbloquear usuarios | ✅ |
| `USUARIO_CAMBIAR_ROL` | Cambiar rol | Modificar rol de usuarios | ✅ |
| `USUARIO_VER_ACTIVIDAD` | Ver actividad | Ver registro de actividades de usuarios | ✅ |
| `USUARIO_RESETEAR_PASSWORD` | Resetear contraseña | Forzar cambio de contraseña | ✅ |

#### 8️⃣ AUDITORÍA (2 permisos) - SOLO ADMIN
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `AUDITORIA_VER` | Ver auditoría | Acceso al registro de auditoría | ✅ |
| `AUDITORIA_EXPORTAR` | Exportar auditoría | Exportar logs de auditoría | ✅ |

#### 9️⃣ SISTEMA (3 permisos) - SOLO ADMIN
| Código | Nombre | Descripción | ¿Crítico? |
|--------|--------|-------------|-----------|
| `SISTEMA_VER_LOGS` | Ver logs | Acceso a logs del sistema | ✅ |
| `SISTEMA_BACKUP` | Backup | Realizar respaldos del sistema | ✅ |
| `SISTEMA_MANTENIMIENTO` | Mantenimiento | Poner sistema en modo mantenimiento | ✅ |

---

