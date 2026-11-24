// T-055 criterio 12: acción «Reenviar comprobante por correo» e historial de entregas, pintados con textContent.
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CODIGO = fs.readFileSync(path.join(raiz,
    'src/main/resources/static/modules/facturacion/js/entregas-correo.js'), 'utf8');

function cargar() {
    const ctx = { window: {} };
    vm.createContext(ctx);
    vm.runInContext(CODIGO, ctx);
    return ctx.window.EntregasCorreo;
}

function nodo(tag) {
    const n = {
        tag, children: [], attrs: {}, listeners: {}, className: '', value: '', disabled: false, _texto: '',
        get textContent() {
            return this._texto + this.children.map((c) => c.textContent).join('');
        },
        set textContent(v) { this._texto = String(v); this.children = []; },
        appendChild(c) { this.children.push(c); return c; },
        setAttribute(k, v) { this.attrs[k] = v; },
        addEventListener(ev, fn) { this.listeners[ev] = fn; },
        querySelector() { return null; }
    };
    Object.defineProperty(n, 'innerHTML', { set() { throw new Error('innerHTML no se debe usar'); } });
    return n;
}
const doc = { createElement: nodo };

function buscar(n, pred, acc = []) {
    if (pred(n)) acc.push(n);
    n.children.forEach((c) => buscar(c, pred, acc));
    return acc;
}
const porTag = (n, tag) => buscar(n, (x) => x.tag === tag);

const CORREO = { comprobanteId: 77, estadoComprobante: 'ACEPTADO', destinatarioPorDefecto: 'cli@ejemplo.com', puedeReenviar: true };

// ---- validación ----

test('correoValido acepta direcciones normales y rechaza el resto', () => {
    const e = cargar();
    for (const v of ['a@b.co', ' cli@ejemplo.com ', 'x.y+z@sub.dominio.cr']) assert.equal(e.correoValido(v), true, v);
    for (const v of ['', null, undefined, 'sin-arroba', 'a@b', 'a b@c.com', '@c.com', 'a@@c.com', 'a@c.com,b@c.com']) {
        assert.equal(e.correoValido(v), false, String(v));
    }
});

test('etiquetas de estado en español, con respaldo al valor crudo', () => {
    const e = cargar();
    assert.equal(e.etiquetaEstado('PENDIENTE'), 'Pendiente');
    assert.equal(e.etiquetaEstado('ENVIADO'), 'Enviado');
    assert.equal(e.etiquetaEstado('FALLIDO'), 'Fallido');
    assert.equal(e.etiquetaEstado('OMITIDO_SIN_CORREO'), 'Omitido: sin correo');
    assert.equal(e.etiquetaEstado('OMITIDO_SANDBOX'), 'Omitido: ambiente de pruebas');
    assert.equal(e.etiquetaEstado('RARO'), 'RARO');
    assert.equal(e.etiquetaEstado(null), '-');
});

test('mensajeError toma message o error del cuerpo y cae al texto por defecto', () => {
    const e = cargar();
    assert.equal(e.mensajeError({ message: 'Correo inválido' }, 'x'), 'Correo inválido');
    assert.equal(e.mensajeError({ error: 'Conflicto' }, 'x'), 'Conflicto');
    assert.equal(e.mensajeError(null, 'x'), 'x');
    assert.equal(e.mensajeError({ message: '  ' }, 'x'), 'x');
});

// ---- visibilidad de la acción ----

test('la acción solo se muestra con correo.puedeReenviar === true', () => {
    const e = cargar();
    assert.equal(e.mostrarAccion(CORREO), true);
    for (const c of [null, undefined, {}, { ...CORREO, puedeReenviar: false }, { ...CORREO, puedeReenviar: 'true' },
        { ...CORREO, comprobanteId: null }]) {
        assert.equal(e.mostrarAccion(c), false);
    }
});

// ---- pintado ----

test('montar: sin permiso no hay formulario pero sí historial; con permiso, el destinatario viene prellenado', async () => {
    const e = cargar();
    const cont = nodo('div');
    await e.montar(doc, cont, { ...CORREO, puedeReenviar: false }, { fetch: async () => ({ ok: true, json: async () => [] }) });
    assert.equal(porTag(cont, 'input').length, 0);
    assert.equal(porTag(cont, 'button').length, 0);

    const cont2 = nodo('div');
    await e.montar(doc, cont2, CORREO, { fetch: async () => ({ ok: true, json: async () => [] }) });
    const input = porTag(cont2, 'input')[0];
    assert.equal(input.value, 'cli@ejemplo.com');
    assert.equal(input.attrs.type, 'email');
    assert.ok(porTag(cont2, 'button').some((b) => b.textContent === 'Reenviar comprobante por correo'));
});

test('montar sin correo (sin comprobante vigente) no pinta nada ni llama al servidor', async () => {
    const e = cargar();
    const cont = nodo('div');
    let llamadas = 0;
    await e.montar(doc, cont, null, { fetch: async () => { llamadas++; return { ok: true, json: async () => [] }; } });
    assert.equal(cont.children.length, 0);
    assert.equal(llamadas, 0);
});

test('historial: columnas destinatario, estado, intentos, fecha y error resumido, todo con textContent', () => {
    const e = cargar();
    const tbody = nodo('tbody');
    e.pintarHistorial(doc, tbody, [{
        id: 1, origen: 'MANUAL', destinatario: 'a@b.co', estado: 'FALLIDO', intentos: 3,
        fechaCreacion: '2026-11-20T10:15:00', fechaEnvio: null, ultimoError: 'HTTP 422 validación'
    }]);
    const celdas = porTag(tbody.children[0], 'td').map((c) => c.textContent);
    assert.equal(celdas[0], 'a@b.co');
    assert.equal(celdas[1], 'Fallido');
    assert.equal(celdas[2], '3');
    assert.match(celdas[3], /20\/11\/2026/);
    assert.equal(celdas[4], 'HTTP 422 validación');
});

test('historial vacío muestra un texto, no una tabla vacía', () => {
    const e = cargar();
    const tbody = nodo('tbody');
    e.pintarHistorial(doc, tbody, []);
    assert.equal(tbody.textContent, 'Sin entregas por correo.');
});

test('destinatario y error con aspecto de HTML se pintan como texto, nunca como marcado', () => {
    const e = cargar();
    const tbody = nodo('tbody');
    const malo = '<img src=x onerror=alert(1)>';
    e.pintarHistorial(doc, tbody, [{ id: 2, destinatario: malo, estado: 'FALLIDO', intentos: 1,
        fechaCreacion: '2026-11-20T10:15:00', ultimoError: '<script>x()</script>' }]);
    const celdas = porTag(tbody.children[0], 'td');
    assert.equal(celdas[0].textContent, malo);
    assert.equal(celdas[4].textContent, '<script>x()</script>');
    assert.equal(buscar(tbody, (n) => n.tag === 'img' || n.tag === 'script').length, 0);
});

// ---- reenvío ----

function conFetch(respuestas) {
    const llamadas = [];
    const fetch = async (url, opts = {}) => {
        llamadas.push({ url, opts });
        const r = respuestas.shift();
        return { ok: r.ok, status: r.status || (r.ok ? 200 : 400), json: async () => r.cuerpo };
    };
    return { fetch, llamadas };
}

test('reenviar: correo inválido no llama al servidor y avisa', async () => {
    const e = cargar();
    const cont = nodo('div');
    const { fetch, llamadas } = conFetch([{ ok: true, cuerpo: [] }]);
    const avisos = [];
    await e.montar(doc, cont, CORREO, { fetch, notify: (t, m) => avisos.push([t, m]), headers: () => ({}) });
    porTag(cont, 'input')[0].value = 'no-es-correo';
    await porTag(cont, 'button')[0].listeners.click();
    assert.equal(llamadas.length, 1); // solo la carga inicial del historial
    assert.equal(avisos[0][0], 'error');
});

test('reenviar: POST con CSRF y destinatario recortado, y el historial se refresca', async () => {
    const e = cargar();
    const cont = nodo('div');
    const { fetch, llamadas } = conFetch([
        { ok: true, cuerpo: [] },
        { ok: true, status: 201, cuerpo: { id: 9 } },
        { ok: true, cuerpo: [{ id: 9, destinatario: 'otro@b.co', estado: 'PENDIENTE', intentos: 0, fechaCreacion: '2026-11-20T10:15:00' }] }
    ]);
    const avisos = [];
    await e.montar(doc, cont, CORREO, { fetch, notify: (t, m) => avisos.push([t, m]), headers: () => ({ 'X-CSRF-TOKEN': 'tok', 'Content-Type': 'application/json' }) });
    porTag(cont, 'input')[0].value = '  otro@b.co ';
    await porTag(cont, 'button')[0].listeners.click();
    assert.equal(llamadas.length, 3);
    assert.equal(llamadas[1].url, '/api/facturas/electronica/comprobantes/77/entregas');
    assert.equal(llamadas[1].opts.method, 'POST');
    assert.equal(llamadas[1].opts.headers['X-CSRF-TOKEN'], 'tok');
    assert.deepEqual(JSON.parse(llamadas[1].opts.body), { destinatario: 'otro@b.co' });
    assert.equal(llamadas[2].url, '/api/facturas/electronica/comprobantes/77/entregas');
    assert.equal(avisos[0][0], 'success');
    assert.ok(porTag(cont, 'td').some((td) => td.textContent === 'otro@b.co'));
});

test('reenviar: el mensaje de error del servidor se muestra tal cual (como texto) y el botón se reactiva', async () => {
    const e = cargar();
    const cont = nodo('div');
    const { fetch } = conFetch([{ ok: true, cuerpo: [] }, { ok: false, status: 409, cuerpo: { message: 'El comprobante no está aceptado' } }]);
    const avisos = [];
    await e.montar(doc, cont, CORREO, { fetch, notify: (t, m) => avisos.push([t, m]), headers: () => ({}) });
    const boton = porTag(cont, 'button')[0];
    await boton.listeners.click();
    assert.deepEqual(avisos[0], ['error', 'El comprobante no está aceptado']);
    assert.equal(boton.disabled, false);
});

test('el módulo no usa innerHTML ni insertAdjacentHTML', () => {
    assert.doesNotMatch(CODIGO, /innerHTML|insertAdjacentHTML|outerHTML/);
});

// ---- panel de inicio y plantillas ----

test('inicio.html: dos tarjetas de entregas con sus enlaces y contadores', () => {
    const html = fs.readFileSync(path.join(raiz, 'src/main/resources/templates/modules/facturacion/inicio.html'), 'utf8');
    assert.match(html, /contadores\.entregasFallidas/);
    assert.match(html, /contadores\.entregasPendientesVencidas/);
    assert.match(html, /\/facturas\/entregas\(estado='FALLIDO'\)/);
    assert.match(html, /\/facturas\/entregas\(estado='PENDIENTE'\)/);
});

test('las tres pantallas cargan el módulo y tienen su contenedor', () => {
    const base = path.join(raiz, 'src/main/resources/templates');
    for (const f of ['modules/facturacion/facturas.html', 'modules/facturacion/notas-credito-detalle.html']) {
        const html = fs.readFileSync(path.join(base, f), 'utf8');
        assert.match(html, /entregas-correo\.js/, f);
    }
    for (const f of ['shared/components/facturas-modal.html', 'modules/facturacion/fragments/factura-modal-detalle.html',
        'modules/facturacion/notas-credito-detalle.html']) {
        assert.match(fs.readFileSync(path.join(base, f), 'utf8'), /id="[a-z-]*correo"/, f);
    }
});

// ---- T-055 revisión 1, criterio 17: los antiguos botones de «Enviar por Email» usan el reenvío de la cola ----

test('enviarReenvio: POST a /entregas con {destinatario} recortado y CSRF; 201 es éxito', async () => {
    const e = cargar();
    const llamadas = [];
    const deps = {
        fetch: async (u, o) => { llamadas.push([u, o]); return { ok: true, status: 201, json: async () => ({ id: 5 }) }; },
        headers: () => ({ 'Content-Type': 'application/json', 'X-CSRF': 't' })
    };
    const r = await e.enviarReenvio(77, ' cli@ejemplo.com ', deps);
    assert.equal(r.ok, true);
    assert.equal(llamadas.length, 1);
    assert.equal(llamadas[0][0], '/api/facturas/electronica/comprobantes/77/entregas');
    assert.equal(llamadas[0][1].method, 'POST');
    assert.equal(llamadas[0][1].headers['X-CSRF'], 't');
    assert.deepEqual(JSON.parse(llamadas[0][1].body), { destinatario: 'cli@ejemplo.com' });
});

test('enviarReenvio: correo inválido no llama a la red; 403, 409 y 400 devuelven un mensaje claro', async () => {
    const e = cargar();
    let n = 0;
    const con = (status, cuerpo) => ({ fetch: async () => { n++; return { ok: false, status, json: async () => cuerpo }; }, headers: () => ({}) });
    const inval = await e.enviarReenvio(1, 'no-es-correo', con(400, {}));
    assert.equal(inval.ok, false);
    assert.equal(n, 0);
    const sinPermiso = await e.enviarReenvio(1, 'a@b.co', con(403, null));
    assert.equal(sinPermiso.ok, false);
    assert.match(sinPermiso.mensaje, /permiso/i);
    const conflicto = await e.enviarReenvio(1, 'a@b.co', con(409, { message: 'Solo comprobantes aceptados' }));
    assert.equal(conflicto.mensaje, 'Solo comprobantes aceptados');
    const red = await e.enviarReenvio(1, 'a@b.co', { fetch: async () => { throw new Error('x'); }, headers: () => ({}) });
    assert.equal(red.ok, false);
});

test('ninguna pantalla llama ya a las rutas SMTP antiguas enviar-email', () => {
    const base = path.join(raiz, 'src/main/resources');
    const archivos = [
        'templates/modules/facturacion/electronica/comprobantes.html',
        'templates/modules/facturacion/facturas.html',
        'static/modules/facturacion/js/facturas.js'
    ];
    for (const a of archivos) {
        const t = fs.readFileSync(path.join(base, a), 'utf8');
        assert.doesNotMatch(t, /enviar-email/, a);
        assert.doesNotMatch(t, /enviarFacturaPorEmail|enviarPorEmail\(/, a);
    }
});

test('comprobantes.html: el botón de correo del modal usa EntregasCorreo.enviarReenvio y exige el permiso', () => {
    const t = fs.readFileSync(path.join(raiz, 'src/main/resources/templates/modules/facturacion/electronica/comprobantes.html'), 'utf8');
    assert.match(t, /entregas-correo\.js/);
    assert.match(t, /EntregasCorreo\.enviarReenvio/);
    assert.match(t, /COMPROBANTE_REENVIAR_CORREO/);
});

test('facturas.html: la opción de correo del listado exige COMPROBANTE_REENVIAR_CORREO y abre el detalle con el reenvío', () => {
    const t = fs.readFileSync(path.join(raiz, 'src/main/resources/templates/modules/facturacion/facturas.html'), 'utf8');
    assert.match(t, /COMPROBANTE_REENVIAR_CORREO/);
    assert.match(t, /Reenviar comprobante por correo/);
});
