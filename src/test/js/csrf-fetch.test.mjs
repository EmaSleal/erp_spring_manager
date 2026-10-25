// T-026 (ADR-031 §1): interceptor global de fetch y $.ajax de shared/js/common.js.
//
// Carga common.js en un contexto vm de Node con window/document simulados (sin dependencias: node:test,
// fetch/Request/Headers nativos) y registra cada petición que llega al fetch original.
//
// Cómo se corre (Node 18 o superior), desde la raíz del repositorio:
//   node --test 'src/test/js/*.test.mjs'
// También lo ejecuta ./mvnw test mediante CommonJsCsrfTest (se omite si no hay node en el PATH).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const COMMON_JS = fs.readFileSync(path.join(raiz, 'src/main/resources/static/shared/js/common.js'), 'utf8');
const ORIGEN = 'http://localhost:8080';
const TOKEN = 'token-de-prueba';
const HEADER = 'X-CSRF-TOKEN';

/**
 * Contexto de navegador mínimo con common.js cargado.
 * @param {{conMeta?: boolean, jquery?: boolean}} opciones
 */
function navegador({ conMeta = true, jquery = false } = {}) {
    const peticiones = [];
    const warns = [];
    const metas = conMeta ? { _csrf: TOKEN, _csrf_header: HEADER } : {};
    const prefiltros = [];
    const ctx = {
        console: { log() {}, error() {}, warn: (...a) => warns.push(a.join(' ')), info() {} },
        location: new URL(ORIGEN + '/facturas'),
        document: {
            querySelector(sel) {
                const m = /meta\[name="([^"]+)"\]/.exec(sel);
                return m && metas[m[1]] !== undefined ? { content: metas[m[1]], getAttribute: () => metas[m[1]] } : null;
            },
            querySelectorAll: () => [],
            addEventListener() {}
        },
        Request, Headers, URL,
        fetch: async (input, init) => {
            peticiones.push({ input, init });
            return new Response('{}', { status: 200 });
        }
    };
    if (jquery) {
        ctx.jQuery = ctx.$ = { ajaxPrefilter: fn => prefiltros.push(fn) };
    }
    ctx.window = ctx;
    vm.createContext(ctx);
    vm.runInContext(COMMON_JS, ctx, { filename: 'common.js' });
    return { ctx, peticiones, warns, prefiltros };
}

/** Encabezados con los que el fetch original recibió la petición (los de init reemplazan a los del Request). */
function encabezados({ input, init }) {
    if (init && init.headers !== undefined) return new Headers(init.headers);
    return input instanceof Request ? input.headers : new Headers();
}

test('POST relativo → lleva el encabezado CSRF', async () => {
    const { ctx, peticiones } = navegador();
    await ctx.fetch('/api/proveedores/1', { method: 'POST', body: '{}' });
    assert.equal(peticiones.length, 1);
    assert.equal(encabezados(peticiones[0]).get(HEADER), TOKEN);
});

test('DELETE con Request → lleva el encabezado CSRF', async () => {
    const { ctx, peticiones } = navegador();
    await ctx.fetch(new Request(ORIGEN + '/api/proveedores/7', { method: 'DELETE' }));
    assert.equal(encabezados(peticiones[0]).get(HEADER), TOKEN);
});

test('POST absoluto del mismo origen y método en minúsculas → lleva el encabezado', async () => {
    const { ctx, peticiones } = navegador();
    await ctx.fetch(ORIGEN + '/api/x', { method: 'post' });
    assert.equal(encabezados(peticiones[0]).get(HEADER), TOKEN);
});

test('GET, HEAD y OPTIONS → sin encabezado CSRF', async () => {
    const { ctx, peticiones } = navegador();
    await ctx.fetch('/api/facturas');
    await ctx.fetch('/api/facturas', { method: 'GET' });
    await ctx.fetch('/api/facturas', { method: 'HEAD' });
    await ctx.fetch('/api/facturas', { method: 'OPTIONS' });
    for (const p of peticiones) assert.equal(encabezados(p).get(HEADER), null);
});

test('POST a otro origen → sin encabezado CSRF', async () => {
    const { ctx, peticiones } = navegador();
    await ctx.fetch('https://api.hacienda.go.cr/recepcion', { method: 'POST' });
    await ctx.fetch('//otro.example/api', { method: 'POST' });
    await ctx.fetch(new Request('https://otro.example/api', { method: 'PUT' }));
    for (const p of peticiones) assert.equal(encabezados(p).get(HEADER), null);
});

test('POST que ya trae el encabezado → no se duplica ni se sobrescribe', async () => {
    const { ctx, peticiones } = navegador();
    await ctx.fetch('/api/x', { method: 'POST', headers: { 'X-CSRF-TOKEN': 'token-propio' } });
    await ctx.fetch('/api/x', { method: 'POST', headers: { 'x-csrf-token': 'token-propio' } });
    for (const p of peticiones) {
        assert.equal(encabezados(p).get(HEADER), 'token-propio');
    }
    // Se pasa tal cual al fetch original
    assert.deepEqual(peticiones[0].init.headers, { 'X-CSRF-TOKEN': 'token-propio' });
});

test('headers como objeto, Headers y array → se conservan y se agrega el CSRF', async () => {
    const { ctx, peticiones } = navegador();
    await ctx.fetch('/api/a', { method: 'PUT', headers: { 'Content-Type': 'application/json' } });
    await ctx.fetch('/api/b', { method: 'PATCH', headers: new Headers({ 'Content-Type': 'application/json' }) });
    await ctx.fetch('/api/c', { method: 'POST', headers: [['Content-Type', 'application/json']] });
    assert.equal(peticiones.length, 3);
    for (const p of peticiones) {
        const h = encabezados(p);
        assert.equal(h.get('Content-Type'), 'application/json');
        assert.equal(h.get(HEADER), TOKEN);
    }
});

test('Request con encabezados propios → se conservan y se agrega el CSRF', async () => {
    const { ctx, peticiones } = navegador();
    await ctx.fetch(new Request(ORIGEN + '/api/r', { method: 'POST', headers: { 'Accept': 'application/json' } }));
    const h = encabezados(peticiones[0]);
    assert.equal(h.get('Accept'), 'application/json');
    assert.equal(h.get(HEADER), TOKEN);
});

test('sin meta _csrf → la petición sale igual y hay un solo console.warn', async () => {
    const { ctx, peticiones, warns } = navegador({ conMeta: false });
    await ctx.fetch('/api/x', { method: 'POST' });
    await ctx.fetch('/api/y', { method: 'DELETE' });
    assert.equal(peticiones.length, 2);
    for (const p of peticiones) assert.equal(encabezados(p).get(HEADER), null);
    assert.equal(warns.filter(w => w.includes('CSRF')).length, 1);
});

test('el interceptor se instala una sola vez', () => {
    const { ctx } = navegador();
    const envuelto = ctx.fetch;
    assert.equal(typeof ctx.instalarCsrfEnFetch, 'function');
    ctx.instalarCsrfEnFetch(ctx);
    assert.equal(ctx.fetch, envuelto);
});

test('jQuery cargado: $.ajaxPrefilter agrega el CSRF a mutaciones de mismo origen y no a GET ni a otro origen', () => {
    const { prefiltros } = navegador({ jquery: true });
    assert.equal(prefiltros.length, 1);
    const enviar = (options) => {
        const puestos = {};
        prefiltros[0](options, {}, { setRequestHeader: (k, v) => { puestos[k] = v; } });
        return puestos;
    };
    assert.deepEqual(enviar({ type: 'PUT', url: '/admin/usuarios/api/1/bloquear', crossDomain: false }), { [HEADER]: TOKEN });
    assert.deepEqual(enviar({ type: 'GET', url: '/api/x', crossDomain: false }), {});
    assert.deepEqual(enviar({ type: 'POST', url: 'https://otro.example/x', crossDomain: true }), {});
    assert.deepEqual(enviar({ type: 'DELETE', url: '/api/x', crossDomain: false, headers: { 'X-CSRF-TOKEN': 'propio' } }), {});
});

test('mensajeErrorHttp muestra el message del 403 JSON de la API', () => {
    const { ctx } = navegador();
    const msg = 'Sesión vencida o token de seguridad inválido; recargue la página';
    assert.equal(ctx.mensajeErrorHttp(403, { status: 403, error: 'Forbidden', message: msg }), msg);
});
