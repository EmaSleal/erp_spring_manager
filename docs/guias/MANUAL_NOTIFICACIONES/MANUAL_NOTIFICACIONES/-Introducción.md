## 📖 Introducción

El **Sistema de Notificaciones** mantiene a los usuarios informados sobre eventos importantes del negocio en tiempo real. Las notificaciones se pueden recibir por múltiples canales según las preferencias del usuario.

### Características Principales

- ✅ **Notificaciones en tiempo real** - WebSocket para actualizaciones instantáneas
- ✅ **Múltiples canales** - Web, Email y WhatsApp
- ✅ **Personalización total** - Configure qué notificaciones recibir
- ✅ **Historial completo** - Vea todas sus notificaciones pasadas
- ✅ **Marcado de leídas** - Controle qué notificaciones ha revisado
- ✅ **Badge de contador** - Vea cuántas notificaciones sin leer tiene
- ✅ **Filtros avanzados** - Busque por tipo, canal, fecha

### ⚙️ Componentes del Sistema

```
┌─────────────────────────────────────────────────┐
│     SISTEMA DE NOTIFICACIONES                  │
├─────────────────────────────────────────────────┤
│                                                 │
│  📱 WEB (WebSocket)                            │
│    └─ Badge + Dropdown en navbar              │
│                                                 │
│  📧 EMAIL (SMTP)                               │
│    └─ Plantillas HTML personalizadas          │
│                                                 │
│  💬 WHATSAPP (API)                             │
│    └─ Plantillas aprobadas por Meta           │
│                                                 │
│  ⚙️ PREFERENCIAS                                │
│    └─ Configuración individual por usuario     │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

