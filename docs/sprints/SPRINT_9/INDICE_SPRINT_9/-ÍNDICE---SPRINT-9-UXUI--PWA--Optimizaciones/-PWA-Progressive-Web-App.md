## 📱 PWA (Progressive Web App)

### Manifest.json

**Configuración completa:**
```json
{
  "name": "WhatsApp Orders Manager",
  "short_name": "WOM ERP",
  "description": "Sistema ERP para gestión de pedidos vía WhatsApp",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#1a1a2e",
  "theme_color": "#4a90e2",
  "orientation": "portrait-primary",
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
      "type": "image/png"
    },
    {
      "src": "/images/icons/icon-128x128.png",
      "sizes": "128x128",
      "type": "image/png"
    },
    {
      "src": "/images/icons/icon-144x144.png",
      "sizes": "144x144",
      "type": "image/png"
    },
    {
      "src": "/images/icons/icon-192x192.png",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/images/icons/icon-512x512.png",
      "sizes": "512x512",
      "type": "image/png"
    }
  ],
  "screenshots": [
    {
      "src": "/images/screenshots/desktop-1.png",
      "sizes": "1280x720",
      "type": "image/png",
      "form_factor": "wide"
    },
    {
      "src": "/images/screenshots/mobile-1.png",
      "sizes": "540x720",
      "type": "image/png",
      "form_factor": "narrow"
    }
  ],
  "categories": ["business", "finance", "productivity"],
  "shortcuts": [
    {
      "name": "Nueva Factura",
      "short_name": "Factura",
      "description": "Crear nueva factura",
      "url": "/facturas/crear",
      "icons": [{ "src": "/images/icons/factura-96.png", "sizes": "96x96" }]
    },
    {
      "name": "Nuevo Cliente",
      "short_name": "Cliente",
      "description": "Registrar nuevo cliente",
      "url": "/clientes/crear",
      "icons": [{ "src": "/images/icons/cliente-96.png", "sizes": "96x96" }]
    }
  ]
}
```

---

### Service Worker

**Estrategia de caché:**
```javascript
// service-worker.js
const CACHE_NAME = 'wom-erp-v1.0.0';
const urlsToCache = [
  '/',
  '/css/styles.css',
  '/css/dark-theme.css',
  '/js/app.js',
  '/images/logo.png',
  '/offline.html'
];

// Instalación
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(urlsToCache))
  );
});

// Activación
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cacheName => {
          if (cacheName !== CACHE_NAME) {
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
});

// Fetch - Cache First para assets, Network First para API
self.addEventListener('fetch', event => {
  if (event.request.url.includes('/api/')) {
    // Network First para API
    event.respondWith(
      fetch(event.request)
        .catch(() => caches.match(event.request))
    );
  } else {
    // Cache First para assets
    event.respondWith(
      caches.match(event.request)
        .then(response => response || fetch(event.request))
    );
  }
});
```

---

### Instalación PWA

**Prompt de instalación:**
```javascript
let deferredPrompt;

window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredPrompt = e;
  
  // Mostrar botón de instalación
  document.getElementById('install-button').style.display = 'block';
});

document.getElementById('install-button').addEventListener('click', async () => {
  if (deferredPrompt) {
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    
    if (outcome === 'accepted') {
      console.log('PWA instalada');
    }
    deferredPrompt = null;
  }
});
```

---

