# 📱 FASE 2: PWA (Progressive Web App)

**Sprint:** 9  
**Fase:** 2 de 5  
**Duración estimada:** 5-7 días  
**Prioridad:** ⭐⭐ ALTA  
**Estado:** 📋 PENDIENTE (0/32 tareas)

---

## 📋 OBJETIVO DE LA FASE

Transformar el sistema en una **Progressive Web App** instalable:
- **Instalable** en móviles (Android/iOS) y escritorio
- **Modo offline** funcional
- **Service Worker** para caché inteligente
- **Sincronización** en segundo plano
- **Notificaciones push** (opcional)
- **Actualización automática**

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/32] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Configuración PWA             [0/6]  ░░░░░░░░░░ 0%
├─ 2. Service Worker y Caché        [0/10] ░░░░░░░░░░ 0%
├─ 3. Modo Offline                  [0/8]  ░░░░░░░░░░ 0%
├─ 4. Instalación                   [0/5]  ░░░░░░░░░░ 0%
└─ 5. Notificaciones Push           [0/3]  ░░░░░░░░░░ 0%
```

---

## 📦 1. CONFIGURACIÓN PWA (6 tareas)

### 1.1. Descripción

Configurar archivos base para PWA:
- **manifest.json** (configuración de la app)
- **Iconos** adaptivos (múltiples tamaños)
- **Splash screens**
- **Meta tags** para PWA

#### Tareas:

- [ ] **1.1** Crear manifest.json

```json
// src/main/resources/static/manifest.json

{
    "name": "WhatsApp Orders Manager",
    "short_name": "WOM ERP",
    "description": "Sistema ERP completo para gestión de órdenes vía WhatsApp",
    "start_url": "/",
    "scope": "/",
    "display": "standalone",
    "theme_color": "#0d6efd",
    "background_color": "#ffffff",
    "orientation": "portrait-primary",
    "lang": "es-CR",
    "dir": "ltr",
    
    "icons": [
        {
            "src": "/images/icons/icon-72x72.png",
            "sizes": "72x72",
            "type": "image/png",
            "purpose": "any maskable"
        },
        {
            "src": "/images/icons/icon-96x96.png",
            "sizes": "96x96",
            "type": "image/png",
            "purpose": "any maskable"
        },
        {
            "src": "/images/icons/icon-128x128.png",
            "sizes": "128x128",
            "type": "image/png",
            "purpose": "any maskable"
        },
        {
            "src": "/images/icons/icon-144x144.png",
            "sizes": "144x144",
            "type": "image/png",
            "purpose": "any maskable"
        },
        {
            "src": "/images/icons/icon-152x152.png",
            "sizes": "152x152",
            "type": "image/png",
            "purpose": "any maskable"
        },
        {
            "src": "/images/icons/icon-192x192.png",
            "sizes": "192x192",
            "type": "image/png",
            "purpose": "any maskable"
        },
        {
            "src": "/images/icons/icon-384x384.png",
            "sizes": "384x384",
            "type": "image/png",
            "purpose": "any maskable"
        },
        {
            "src": "/images/icons/icon-512x512.png",
            "sizes": "512x512",
            "type": "image/png",
            "purpose": "any maskable"
        }
    ],
    
    "screenshots": [
        {
            "src": "/images/screenshots/screenshot1.png",
            "sizes": "540x720",
            "type": "image/png",
            "label": "Dashboard principal"
        },
        {
            "src": "/images/screenshots/screenshot2.png",
            "sizes": "540x720",
            "type": "image/png",
            "label": "Gestión de facturas"
        }
    ],
    
    "shortcuts": [
        {
            "name": "Nueva Factura",
            "short_name": "Factura",
            "description": "Crear nueva factura rápidamente",
            "url": "/facturas/nueva",
            "icons": [
                {
                    "src": "/images/icons/factura-shortcut.png",
                    "sizes": "96x96"
                }
            ]
        },
        {
            "name": "Clientes",
            "short_name": "Clientes",
            "url": "/clientes",
            "icons": [
                {
                    "src": "/images/icons/cliente-shortcut.png",
                    "sizes": "96x96"
                }
            ]
        }
    ],
    
    "categories": ["business", "finance", "productivity"],
    
    "related_applications": [],
    "prefer_related_applications": false
}
```

- [ ] **1.2** Agregar meta tags PWA en head

```html
<!-- En layout.html o head común -->
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    
    <!-- PWA Meta Tags -->
    <meta name="application-name" content="WOM ERP">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="default">
    <meta name="apple-mobile-web-app-title" content="WOM ERP">
    <meta name="description" content="Sistema ERP para gestión de órdenes vía WhatsApp">
    <meta name="format-detection" content="telephone=no">
    <meta name="mobile-web-app-capable" content="yes">
    <meta name="theme-color" content="#0d6efd">
    
    <!-- Apple Touch Icons -->
    <link rel="apple-touch-icon" href="/images/icons/icon-152x152.png">
    <link rel="apple-touch-icon" sizes="180x180" href="/images/icons/icon-180x180.png">
    
    <!-- Manifest -->
    <link rel="manifest" href="/manifest.json">
    
    <!-- Favicon -->
    <link rel="icon" type="image/png" sizes="32x32" href="/favicon-32x32.png">
    <link rel="icon" type="image/png" sizes="16x16" href="/favicon-16x16.png">
    
    <title>WhatsApp Orders Manager</title>
</head>
<body>
    <!-- ... -->
</body>
</html>
```

- [ ] **1.3** Generar iconos en múltiples tamaños

```bash
# Script para generar iconos desde imagen base
# Usar herramienta como https://realfavicongenerator.net/
# O ImageMagick:

# 72x72
magick convert icon-base.png -resize 72x72 icon-72x72.png

# 96x96
magick convert icon-base.png -resize 96x96 icon-96x96.png

# 128x128
magick convert icon-base.png -resize 128x128 icon-128x128.png

# 144x144
magick convert icon-base.png -resize 144x144 icon-144x144.png

# 152x152
magick convert icon-base.png -resize 152x152 icon-152x152.png

# 192x192
magick convert icon-base.png -resize 192x192 icon-192x192.png

# 384x384
magick convert icon-base.png -resize 384x384 icon-384x384.png

# 512x512
magick convert icon-base.png -resize 512x512 icon-512x512.png
```

**Ubicación:** `src/main/resources/static/images/icons/`

- [ ] **1.4** Crear splash screens para iOS

```html
<!-- Splash screens iOS -->
<link rel="apple-touch-startup-image" 
      href="/images/splash/iphone5_splash.png" 
      media="(device-width: 320px) and (device-height: 568px) and (-webkit-device-pixel-ratio: 2)">
      
<link rel="apple-touch-startup-image" 
      href="/images/splash/iphone6_splash.png" 
      media="(device-width: 375px) and (device-height: 667px) and (-webkit-device-pixel-ratio: 2)">
      
<link rel="apple-touch-startup-image" 
      href="/images/splash/iphoneplus_splash.png" 
      media="(device-width: 621px) and (device-height: 1104px) and (-webkit-device-pixel-ratio: 3)">
      
<link rel="apple-touch-startup-image" 
      href="/images/splash/iphonex_splash.png" 
      media="(device-width: 375px) and (device-height: 812px) and (-webkit-device-pixel-ratio: 3)">
```

- [ ] **1.5** Configurar Controller para servir manifest.json

```java
@Controller
public class PWAController {
    
    @GetMapping(value = "/manifest.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getManifest() throws IOException {
        // Servir manifest.json desde resources/static
        Resource resource = new ClassPathResource("static/manifest.json");
        return new String(resource.getInputStream().readAllBytes());
    }
}
```

- [ ] **1.6** Registrar Service Worker en página principal

```javascript
// Al final del <body> en layout.html
<script>
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('/service-worker.js')
            .then(registration => {
                console.log('✅ Service Worker registrado:', registration.scope);
            })
            .catch(error => {
                console.error('❌ Error al registrar Service Worker:', error);
            });
    });
}
</script>
```

---

## 📦 2. SERVICE WORKER Y CACHÉ (10 tareas)

### 2.1. Descripción

Implementar **Service Worker** para:
- Caché de assets estáticos (CSS, JS, imágenes)
- Caché de datos (facturas, clientes)
- Estrategias: Cache First, Network First, Stale While Revalidate
- Actualización automática

#### Tareas:

- [ ] **2.1** Crear service-worker.js

```javascript
// src/main/resources/static/service-worker.js

const CACHE_NAME = 'wom-erp-v1.0.0';
const STATIC_CACHE = 'static-v1';
const DYNAMIC_CACHE = 'dynamic-v1';

// Archivos a cachear (cache static)
const STATIC_ASSETS = [
    '/',
    '/css/bootstrap.min.css',
    '/css/main.css',
    '/css/themes/light-theme.css',
    '/css/themes/dark-theme.css',
    '/js/bootstrap.bundle.min.js',
    '/js/jquery-3.6.0.min.js',
    '/js/theme-switcher.js',
    '/js/accessibility.js',
    '/images/logo.png',
    '/manifest.json',
    '/offline.html'
];

// Instalar Service Worker
self.addEventListener('install', (event) => {
    console.log('🔧 Service Worker: Instalando...');
    
    event.waitUntil(
        caches.open(STATIC_CACHE)
            .then(cache => {
                console.log('📦 Cacheando archivos estáticos...');
                return cache.addAll(STATIC_ASSETS);
            })
            .then(() => self.skipWaiting())
    );
});

// Activar Service Worker
self.addEventListener('activate', (event) => {
    console.log('✅ Service Worker: Activado');
    
    event.waitUntil(
        caches.keys().then(cacheNames => {
            return Promise.all(
                cacheNames.map(cache => {
                    if (cache !== STATIC_CACHE && cache !== DYNAMIC_CACHE) {
                        console.log('🗑️ Eliminando caché antigua:', cache);
                        return caches.delete(cache);
                    }
                })
            );
        })
        .then(() => self.clients.claim())
    );
});

// Interceptar peticiones
self.addEventListener('fetch', (event) => {
    const { request } = event;
    const url = new URL(request.url);
    
    // Estrategia según tipo de recurso
    if (url.origin === location.origin) {
        // Recursos del mismo origen
        if (isStaticAsset(request)) {
            // Cache First para assets estáticos
            event.respondWith(cacheFirst(request));
        } else if (isAPIRequest(request)) {
            // Network First para API
            event.respondWith(networkFirst(request));
        } else {
            // Stale While Revalidate para páginas
            event.respondWith(staleWhileRevalidate(request));
        }
    }
});

/**
 * Estrategia: Cache First (para assets estáticos)
 */
async function cacheFirst(request) {
    const cache = await caches.open(STATIC_CACHE);
    const cached = await cache.match(request);
    
    if (cached) {
        return cached;
    }
    
    try {
        const response = await fetch(request);
        cache.put(request, response.clone());
        return response;
    } catch (error) {
        console.error('Error en Cache First:', error);
        return caches.match('/offline.html');
    }
}

/**
 * Estrategia: Network First (para API)
 */
async function networkFirst(request) {
    const cache = await caches.open(DYNAMIC_CACHE);
    
    try {
        const response = await fetch(request);
        cache.put(request, response.clone());
        return response;
    } catch (error) {
        const cached = await cache.match(request);
        if (cached) {
            return cached;
        }
        return new Response(
            JSON.stringify({ error: 'Sin conexión' }), 
            { 
                status: 503, 
                headers: { 'Content-Type': 'application/json' } 
            }
        );
    }
}

/**
 * Estrategia: Stale While Revalidate (para páginas)
 */
async function staleWhileRevalidate(request) {
    const cache = await caches.open(DYNAMIC_CACHE);
    const cached = await cache.match(request);
    
    const fetchPromise = fetch(request).then(response => {
        cache.put(request, response.clone());
        return response;
    });
    
    return cached || fetchPromise;
}

/**
 * Verificar si es asset estático
 */
function isStaticAsset(request) {
    return request.url.match(/\.(css|js|png|jpg|jpeg|svg|woff2?|ttf|eot)$/);
}

/**
 * Verificar si es petición a API
 */
function isAPIRequest(request) {
    return request.url.includes('/api/');
}
```

- [ ] **2.2** Crear página offline.html

```html
<!-- src/main/resources/static/offline.html -->
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sin Conexión - WOM ERP</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100vh;
            margin: 0;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            text-align: center;
        }
        .container {
            max-width: 500px;
            padding: 2rem;
        }
        h1 {
            font-size: 4rem;
            margin: 0;
        }
        p {
            font-size: 1.2rem;
            margin: 1rem 0;
        }
        .icon {
            font-size: 6rem;
            margin-bottom: 2rem;
        }
        button {
            background: white;
            color: #667eea;
            border: none;
            padding: 1rem 2rem;
            font-size: 1rem;
            border-radius: 0.5rem;
            cursor: pointer;
            margin-top: 2rem;
        }
        button:hover {
            background: #f0f0f0;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="icon">📡</div>
        <h1>Sin Conexión</h1>
        <p>No tienes conexión a internet en este momento.</p>
        <p>Algunos datos pueden estar disponibles en modo offline.</p>
        <button onclick="window.location.reload()">
            Intentar de nuevo
        </button>
    </div>
</body>
</html>
```

- [ ] **2.3** Implementar versionado de caché

```javascript
// En service-worker.js

// Incrementar versión al actualizar
const VERSION = '1.0.1';
const CACHE_NAME = `wom-erp-v${VERSION}`;

// Al activar, eliminar cachés antiguas
self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then(cacheNames => {
            return Promise.all(
                cacheNames
                    .filter(cache => cache.startsWith('wom-erp-') && cache !== CACHE_NAME)
                    .map(cache => {
                        console.log('🗑️ Eliminando caché antigua:', cache);
                        return caches.delete(cache);
                    })
            );
        })
    );
});
```

- [ ] **2.4** Caché de imágenes con límite de tamaño

```javascript
// Limitar tamaño de caché de imágenes
const MAX_IMAGE_CACHE_SIZE = 50; // Máximo 50 imágenes

async function cacheImage(request, response) {
    const cache = await caches.open('images-cache');
    const keys = await cache.keys();
    
    if (keys.length >= MAX_IMAGE_CACHE_SIZE) {
        // Eliminar la más antigua
        await cache.delete(keys[0]);
    }
    
    await cache.put(request, response);
}
```

- [ ] **2.5** Implementar Background Sync

```javascript
// Background Sync para operaciones offline
self.addEventListener('sync', (event) => {
    if (event.tag === 'sync-facturas') {
        event.waitUntil(syncFacturas());
    }
});

async function syncFacturas() {
    // Obtener operaciones pendientes de IndexedDB
    const pending = await getPendingOperations();
    
    for (const operation of pending) {
        try {
            await fetch(operation.url, {
                method: operation.method,
                body: operation.data
            });
            
            // Marcar como sincronizada
            await markAsSynced(operation.id);
        } catch (error) {
            console.error('Error al sincronizar:', error);
        }
    }
}
```

- [ ] **2.6** Notificación de actualización disponible

```javascript
// Detectar nueva versión disponible
self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SKIP_WAITING') {
        self.skipWaiting();
    }
});

// En el cliente (página)
navigator.serviceWorker.addEventListener('controllerchange', () => {
    // Mostrar notificación de actualización
    if (confirm('Nueva versión disponible. ¿Recargar página?')) {
        window.location.reload();
    }
});
```

- [ ] **2.7** Precaché de rutas críticas
- [ ] **2.8** Estrategia de caché para fuentes web
- [ ] **2.9** Caché de respuestas API con expiración
- [ ] **2.10** Tests de Service Worker

---

## 📦 3. MODO OFFLINE (8 tareas)

- [ ] **3.1** Detectar estado de conexión

```javascript
// src/main/resources/static/js/offline-manager.js

class OfflineManager {
    
    constructor() {
        this.isOnline = navigator.onLine;
        this.setupListeners();
        this.showStatus();
    }
    
    setupListeners() {
        window.addEventListener('online', () => {
            this.isOnline = true;
            this.showStatus();
            this.syncPendingOperations();
        });
        
        window.addEventListener('offline', () => {
            this.isOnline = false;
            this.showStatus();
        });
    }
    
    showStatus() {
        const banner = document.getElementById('offline-banner');
        if (!banner) return;
        
        if (this.isOnline) {
            banner.classList.remove('show');
        } else {
            banner.classList.add('show');
        }
    }
    
    async syncPendingOperations() {
        // Sincronizar operaciones pendientes
        if ('serviceWorker' in navigator && 'sync' in registration) {
            const registration = await navigator.serviceWorker.ready;
            await registration.sync.register('sync-facturas');
        }
    }
}

const offlineManager = new OfflineManager();
```

- [ ] **3.2** Banner de estado offline

```html
<!-- Banner de estado offline -->
<div id="offline-banner" class="offline-banner">
    <div class="container">
        <i class="bi bi-wifi-off me-2"></i>
        Sin conexión a internet. Trabajando en modo offline.
    </div>
</div>
```

```css
.offline-banner {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    background: #ff6b6b;
    color: white;
    padding: 0.75rem;
    text-align: center;
    z-index: 9999;
    transform: translateY(-100%);
    transition: transform 0.3s;
}

.offline-banner.show {
    transform: translateY(0);
}
```

- [ ] **3.3** IndexedDB para almacenamiento offline

```javascript
// src/main/resources/static/js/indexed-db.js

class OfflineDB {
    
    constructor() {
        this.dbName = 'wom-erp-offline';
        this.version = 1;
        this.db = null;
    }
    
    async init() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.dbName, this.version);
            
            request.onerror = () => reject(request.error);
            request.onsuccess = () => {
                this.db = request.result;
                resolve(this.db);
            };
            
            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                
                // Crear object stores
                if (!db.objectStoreNames.contains('facturas')) {
                    db.createObjectStore('facturas', { keyPath: 'id', autoIncrement: true });
                }
                
                if (!db.objectStoreNames.contains('clientes')) {
                    db.createObjectStore('clientes', { keyPath: 'id', autoIncrement: true });
                }
                
                if (!db.objectStoreNames.contains('pending-operations')) {
                    db.createObjectStore('pending-operations', { keyPath: 'id', autoIncrement: true });
                }
            };
        });
    }
    
    async save(storeName, data) {
        const tx = this.db.transaction(storeName, 'readwrite');
        const store = tx.objectStore(storeName);
        return store.put(data);
    }
    
    async getAll(storeName) {
        const tx = this.db.transaction(storeName, 'readonly');
        const store = tx.objectStore(storeName);
        return store.getAll();
    }
    
    async delete(storeName, id) {
        const tx = this.db.transaction(storeName, 'readwrite');
        const store = tx.objectStore(storeName);
        return store.delete(id);
    }
}

const offlineDB = new OfflineDB();
offlineDB.init();
```

- [ ] **3.4** Queue de operaciones pendientes offline
- [ ] **3.5** Sincronización automática al reconectar
- [ ] **3.6** Indicador visual de datos cacheados
- [ ] **3.7** Fallback para formularios offline
- [ ] **3.8** Tests de funcionalidad offline

---

## 📦 4. INSTALACIÓN (5 tareas)

- [ ] **4.1** Banner "Agregar a pantalla de inicio" (Android)

```javascript
// Prompt de instalación
let deferredPrompt;

window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    deferredPrompt = e;
    
    // Mostrar banner personalizado
    showInstallBanner();
});

function showInstallBanner() {
    const banner = document.getElementById('install-banner');
    banner.style.display = 'block';
}

async function installApp() {
    if (!deferredPrompt) return;
    
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    
    console.log(`Resultado de instalación: ${outcome}`);
    deferredPrompt = null;
    
    document.getElementById('install-banner').style.display = 'none';
}
```

- [ ] **4.2** Instrucciones para iOS (Safari)
- [ ] **4.3** Tracking de instalaciones
- [ ] **4.4** Standalone mode detection
- [ ] **4.5** Tests de instalación en múltiples plataformas

---

## 📦 5. NOTIFICACIONES PUSH (3 tareas - OPCIONAL)

- [ ] **5.1** Solicitar permisos de notificaciones
- [ ] **5.2** Implementar push notifications (backend)
- [ ] **5.3** Manejar notificaciones en Service Worker

---

## 📊 CRITERIOS DE ACEPTACIÓN

✅ **Manifest.json configurado**  
✅ **Service Worker funcionando**  
✅ **Caché offline operativa**  
✅ **Instalable en Android**  
✅ **Instalable en iOS (con instrucciones)**  
✅ **Instalable en escritorio (Chrome/Edge)**  
✅ **Modo offline funcional**  
✅ **Sincronización en segundo plano**  
✅ **Lighthouse PWA score ≥ 90**  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprint 9 Fase 1: UX/UI

**Habilita:**
- 🚀 Fase 3: Optimizaciones

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** PWA Team  
**Prioridad:** ALTA - App instalable
