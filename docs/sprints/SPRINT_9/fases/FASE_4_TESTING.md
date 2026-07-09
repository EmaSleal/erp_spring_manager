# 🧪 FASE 4: Testing y Validación

**Sprint:** 9  
**Fase:** 4 de 5  
**Duración estimada:** 2-3 días  
**Prioridad:** ⭐ MEDIA  
**Estado:** 📋 PENDIENTE (0/5 tareas)

---

## 📋 OBJETIVO DE LA FASE

Validar calidad de mejoras de UX/UI, PWA y optimizaciones:
- **Tests de accesibilidad** (WCAG 2.1 AA)
- **Tests de PWA** (Lighthouse)
- **Tests de rendimiento**
- **Tests de tema oscuro**
- **Validación offline**

---

## 📊 PROGRESO GENERAL

```
Progreso: [0/5] ░░░░░░░░░░░░░░░░░░░░ 0%

├─ 1. Tests de Accesibilidad        [0/1]  ░░░░░░░░░░ 0%
├─ 2. Tests de PWA                  [0/1]  ░░░░░░░░░░ 0%
├─ 3. Tests de Rendimiento          [0/1]  ░░░░░░░░░░ 0%
├─ 4. Tests de Tema Oscuro          [0/1]  ░░░░░░░░░░ 0%
└─ 5. Tests de Funcionalidad Offline [0/1] ░░░░░░░░░░ 0%
```

---

## 📦 1. TESTS DE ACCESIBILIDAD (1 tarea)

### Objetivo:
Validar cumplimiento **WCAG 2.1 Nivel AA**

### Tareas:

- [ ] **1.1** Tests con axe-core DevTools

```javascript
// tests/accessibility.test.js
const { AxePuppeteer } = require('@axe-core/puppeteer');
const puppeteer = require('puppeteer');

describe('Tests de Accesibilidad', () => {
    let browser;
    let page;
    
    beforeAll(async () => {
        browser = await puppeteer.launch();
        page = await browser.newPage();
    });
    
    afterAll(async () => {
        await browser.close();
    });
    
    test('Dashboard debe cumplir WCAG 2.1 AA', async () => {
        await page.goto('http://localhost:8080');
        
        const results = await new AxePuppeteer(page)
            .withTags(['wcag2a', 'wcag2aa'])
            .analyze();
        
        expect(results.violations).toHaveLength(0);
    });
    
    test('Listado de facturas debe cumplir WCAG 2.1 AA', async () => {
        await page.goto('http://localhost:8080/facturas');
        
        const results = await new AxePuppeteer(page).analyze();
        
        expect(results.violations).toHaveLength(0);
    });
    
    test('Formulario de factura debe cumplir WCAG 2.1 AA', async () => {
        await page.goto('http://localhost:8080/facturas/nueva');
        
        const results = await new AxePuppeteer(page).analyze();
        
        expect(results.violations).toHaveLength(0);
    });
    
    test('Contraste de colores en tema claro', async () => {
        await page.goto('http://localhost:8080');
        await page.evaluate(() => {
            document.documentElement.setAttribute('data-theme', 'light');
        });
        
        const results = await new AxePuppeteer(page)
            .withTags(['color-contrast'])
            .analyze();
        
        expect(results.violations).toHaveLength(0);
    });
    
    test('Contraste de colores en tema oscuro', async () => {
        await page.goto('http://localhost:8080');
        await page.evaluate(() => {
            document.documentElement.setAttribute('data-theme', 'dark');
        });
        
        const results = await new AxePuppeteer(page)
            .withTags(['color-contrast'])
            .analyze();
        
        expect(results.violations).toHaveLength(0);
    });
});
```

**Criterios:**
- ✅ 0 violaciones WCAG 2.1 AA
- ✅ Contraste de colores ≥ 4.5:1
- ✅ ARIA labels presentes
- ✅ Navegación por teclado funcional

---

## 📦 2. TESTS DE PWA (1 tarea)

### Objetivo:
Validar instalación y funcionalidad PWA

### Tareas:

- [ ] **2.1** Tests con Lighthouse CI

```javascript
// lighthouse.config.js
module.exports = {
    ci: {
        collect: {
            url: [
                'http://localhost:8080/',
                'http://localhost:8080/facturas',
                'http://localhost:8080/clientes'
            ],
            numberOfRuns: 3
        },
        assert: {
            preset: 'lighthouse:recommended',
            assertions: {
                // PWA
                'installable-manifest': ['error', { minScore: 1 }],
                'service-worker': ['error', { minScore: 1 }],
                'splash-screen': ['error', { minScore: 1 }],
                'themed-omnibox': ['error', { minScore: 1 }],
                'viewport': ['error', { minScore: 1 }],
                
                // Performance
                'first-contentful-paint': ['error', { maxNumericValue: 1800 }],
                'largest-contentful-paint': ['error', { maxNumericValue: 2500 }],
                'interactive': ['error', { maxNumericValue: 3800 }],
                'cumulative-layout-shift': ['error', { maxNumericValue: 0.1 }],
                
                // Best Practices
                'errors-in-console': ['error', { minScore: 0.9 }],
                'is-on-https': ['error', { minScore: 1 }],
                
                // SEO
                'document-title': ['error', { minScore: 1 }],
                'meta-description': ['error', { minScore: 1 }],
                
                // Accessibility
                'color-contrast': ['error', { minScore: 0.9 }],
                'html-has-lang': ['error', { minScore: 1 }],
                'label': ['error', { minScore: 0.9 }]
            }
        },
        upload: {
            target: 'temporary-public-storage'
        }
    }
};
```

**Tests manuales:**

1. **Android (Chrome):**
   - [ ] Abrir sitio en Chrome móvil
   - [ ] Verificar banner "Agregar a pantalla de inicio"
   - [ ] Instalar app
   - [ ] Abrir app instalada (standalone mode)
   - [ ] Verificar funcionamiento offline
   - [ ] Verificar icono y splash screen

2. **iOS (Safari):**
   - [ ] Abrir sitio en Safari
   - [ ] Tap en "Compartir"
   - [ ] Tap en "Agregar a pantalla de inicio"
   - [ ] Verificar icono en home screen
   - [ ] Abrir app
   - [ ] Verificar funcionamiento

3. **Desktop (Chrome/Edge):**
   - [ ] Abrir sitio
   - [ ] Click en icono de instalación en barra de direcciones
   - [ ] Instalar app
   - [ ] Abrir app instalada
   - [ ] Verificar funcionamiento

**Criterios:**
- ✅ Lighthouse PWA score ≥ 90
- ✅ Manifest.json válido
- ✅ Service Worker registrado
- ✅ Instalable en móvil y desktop
- ✅ Splash screen visible
- ✅ Iconos correctos

---

## 📦 3. TESTS DE RENDIMIENTO (1 tarea)

### Objetivo:
Validar tiempos de carga y métricas Core Web Vitals

### Tareas:

- [ ] **3.1** Tests de performance con Lighthouse

```javascript
// tests/performance.test.js
const lighthouse = require('lighthouse');
const chromeLauncher = require('chrome-launcher');

async function runLighthouse(url) {
    const chrome = await chromeLauncher.launch({ chromeFlags: ['--headless'] });
    
    const options = {
        logLevel: 'info',
        output: 'json',
        port: chrome.port
    };
    
    const runnerResult = await lighthouse(url, options);
    
    await chrome.kill();
    
    return runnerResult.lhr;
}

describe('Tests de Rendimiento', () => {
    
    test('Dashboard debe cargar rápido', async () => {
        const result = await runLighthouse('http://localhost:8080/');
        
        const metrics = result.audits;
        
        // First Contentful Paint < 1.8s
        expect(metrics['first-contentful-paint'].numericValue).toBeLessThan(1800);
        
        // Largest Contentful Paint < 2.5s
        expect(metrics['largest-contentful-paint'].numericValue).toBeLessThan(2500);
        
        // Time to Interactive < 3.8s
        expect(metrics['interactive'].numericValue).toBeLessThan(3800);
        
        // Cumulative Layout Shift < 0.1
        expect(metrics['cumulative-layout-shift'].numericValue).toBeLessThan(0.1);
        
        // Performance score > 90
        expect(result.categories.performance.score).toBeGreaterThanOrEqual(0.9);
    });
    
    test('Listado de facturas debe tener buen rendimiento', async () => {
        const result = await runLighthouse('http://localhost:8080/facturas');
        
        expect(result.categories.performance.score).toBeGreaterThanOrEqual(0.85);
    });
});
```

**Criterios:**
- ✅ Performance score > 90
- ✅ FCP < 1.8s
- ✅ LCP < 2.5s
- ✅ TTI < 3.8s
- ✅ CLS < 0.1
- ✅ Total Blocking Time < 200ms

---

## 📦 4. TESTS DE TEMA OSCURO (1 tarea)

### Objetivo:
Validar funcionamiento de tema oscuro

### Tareas:

- [ ] **4.1** Tests de cambio de tema

```javascript
// tests/theme.test.js
describe('Tests de Tema Oscuro', () => {
    
    test('Debe cambiar a tema oscuro', async () => {
        await page.goto('http://localhost:8080');
        
        // Click en botón de tema
        await page.click('#theme-toggle');
        
        // Verificar que se aplicó tema oscuro
        const theme = await page.evaluate(() => {
            return document.documentElement.getAttribute('data-theme');
        });
        
        expect(theme).toBe('dark');
    });
    
    test('Debe persistir preferencia de tema', async () => {
        await page.goto('http://localhost:8080');
        
        // Cambiar a tema oscuro
        await page.click('#theme-toggle');
        
        // Recargar página
        await page.reload();
        
        // Verificar que se mantiene tema oscuro
        const theme = await page.evaluate(() => {
            return document.documentElement.getAttribute('data-theme');
        });
        
        expect(theme).toBe('dark');
    });
    
    test('Todos los elementos deben ser visibles en tema oscuro', async () => {
        await page.goto('http://localhost:8080');
        
        // Cambiar a tema oscuro
        await page.evaluate(() => {
            document.documentElement.setAttribute('data-theme', 'dark');
        });
        
        // Verificar contraste
        const results = await new AxePuppeteer(page)
            .withTags(['color-contrast'])
            .analyze();
        
        expect(results.violations).toHaveLength(0);
    });
    
    test('Selector de tema debe funcionar', async () => {
        await page.goto('http://localhost:8080');
        
        // Abrir dropdown de tema
        await page.click('#themeDropdown');
        
        // Seleccionar tema oscuro
        await page.click('a[onclick*="setTheme(\'dark\')"]');
        
        // Verificar cambio
        const theme = await page.evaluate(() => {
            return document.documentElement.getAttribute('data-theme');
        });
        
        expect(theme).toBe('dark');
    });
});
```

**Criterios:**
- ✅ Cambio de tema funcional
- ✅ Persistencia en localStorage
- ✅ Contraste adecuado en ambos temas
- ✅ Transición suave
- ✅ Meta theme-color actualizada

---

## 📦 5. TESTS DE FUNCIONALIDAD OFFLINE (1 tarea)

### Objetivo:
Validar que la app funcione offline

### Tareas:

- [ ] **5.1** Tests de modo offline

```javascript
// tests/offline.test.js
describe('Tests de Modo Offline', () => {
    
    test('Service Worker debe registrarse correctamente', async () => {
        await page.goto('http://localhost:8080');
        
        const swRegistered = await page.evaluate(async () => {
            const registration = await navigator.serviceWorker.getRegistration();
            return registration !== undefined;
        });
        
        expect(swRegistered).toBe(true);
    });
    
    test('Debe mostrar contenido cacheado offline', async () => {
        await page.goto('http://localhost:8080');
        
        // Esperar que se cachee contenido
        await page.waitForTimeout(2000);
        
        // Simular offline
        await page.setOfflineMode(true);
        
        // Recargar página
        await page.reload();
        
        // Verificar que carga (desde caché)
        const title = await page.title();
        expect(title).toBeTruthy();
    });
    
    test('Debe mostrar banner offline', async () => {
        await page.goto('http://localhost:8080');
        
        // Simular offline
        await page.setOfflineMode(true);
        
        // Esperar que aparezca banner
        await page.waitForSelector('.offline-banner.show', { timeout: 5000 });
        
        const bannerVisible = await page.isVisible('.offline-banner.show');
        expect(bannerVisible).toBe(true);
    });
    
    test('Debe sincronizar al volver online', async () => {
        await page.goto('http://localhost:8080');
        
        // Simular offline
        await page.setOfflineMode(true);
        
        // Crear operación offline (guardaría en IndexedDB)
        // ...
        
        // Volver online
        await page.setOfflineMode(false);
        
        // Esperar sincronización
        await page.waitForTimeout(2000);
        
        // Verificar que se sincronizó
        // ...
    });
});
```

**Tests manuales:**

1. **Modo offline completo:**
   - [ ] Desconectar WiFi/Datos
   - [ ] Intentar abrir app instalada
   - [ ] Verificar que carga contenido cacheado
   - [ ] Verificar banner offline visible
   - [ ] Intentar navegar entre páginas
   - [ ] Reconectar y verificar sincronización

**Criterios:**
- ✅ Service Worker registrado
- ✅ Contenido cacheado accesible offline
- ✅ Banner offline visible
- ✅ Sincronización al reconectar
- ✅ IndexedDB funcionando

---

## 📊 CRITERIOS DE ACEPTACIÓN GLOBAL

✅ **Accesibilidad:** 0 violaciones WCAG 2.1 AA  
✅ **PWA:** Lighthouse PWA score ≥ 90  
✅ **Rendimiento:** Lighthouse Performance score > 90  
✅ **FCP < 1.8s, LCP < 2.5s, TTI < 3.8s, CLS < 0.1**  
✅ **Tema oscuro:** Funcionando en todas las páginas  
✅ **Offline:** Contenido cacheado accesible  

---

## 📚 DEPENDENCIAS

**Requiere:**
- ✅ Sprint 9 Fase 1: UX/UI
- ✅ Sprint 9 Fase 2: PWA
- ✅ Sprint 9 Fase 3: Optimizaciones

**Habilita:**
- 🚀 Fase 5: Documentación

---

## 🛠️ HERRAMIENTAS DE TESTING

- **axe-core DevTools** - Accesibilidad
- **Lighthouse CI** - PWA y Performance
- **Puppeteer** - Tests automatizados
- **Chrome DevTools** - Manual testing
- **BrowserStack** - Cross-browser testing

---

**Fase creada:** 16 de enero de 2026  
**Responsable:** QA Team  
**Prioridad:** MEDIA - Validación de calidad
