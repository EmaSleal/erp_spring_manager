// T-044 criterio 8: lo que la UI pinta del ambiente del comprobante sale de las banderas del servidor.
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CODIGO = fs.readFileSync(path.join(raiz,
    'src/main/resources/static/modules/facturacion/js/ambiente-comprobante.js'), 'utf8');
const MOTIVO = 'El comprobante pertenece al ambiente SANDBOX y la configuración activa es PRODUCCION; no se envía';

// Los objetos creados dentro del contexto vm tienen otro prototipo: se comparan como JSON
const plano = (v) => JSON.parse(JSON.stringify(v));

function cargar() {
    const ctx = { window: {} };
    vm.createContext(ctx);
    vm.runInContext(CODIGO, ctx);
    return ctx.window.AmbienteComprobante;
}

test('etiqueta: SANDBOX, PRODUCCION y sin ambiente', () => {
    const a = cargar();
    assert.equal(a.etiqueta('SANDBOX').texto, 'Sandbox');
    assert.equal(a.etiqueta('PRODUCCION').texto, 'Producción');
    assert.notEqual(a.etiqueta('SANDBOX').clase, a.etiqueta('PRODUCCION').clase);
    for (const v of [null, undefined, '', 'OTRO']) {
        assert.equal(a.etiqueta(v).texto, 'Sin ambiente');
    }
});

test('bloqueo: solo cuando el servidor dice que no coincide, con su motivo fijo', () => {
    const a = cargar();
    assert.deepEqual(plano(a.bloqueo({ ambienteCoincide: true, motivoAmbiente: null })), { bloqueado: false, motivo: null });
    assert.deepEqual(plano(a.bloqueo({})), { bloqueado: false, motivo: null });
    assert.deepEqual(plano(a.bloqueo(null)), { bloqueado: false, motivo: null });
    assert.deepEqual(plano(a.bloqueo({ ambienteCoincide: false, motivoAmbiente: MOTIVO })), { bloqueado: true, motivo: MOTIVO });
});

test('estadoAccion: sin botón si el estado no lo permite; deshabilitado con el motivo si el ambiente no coincide', () => {
    const a = cargar();
    const otro = { ambienteCoincide: false, motivoAmbiente: MOTIVO };
    assert.equal(a.estadoAccion(false, otro), null);
    assert.equal(a.estadoAccion(false, { ambienteCoincide: true }), null);
    assert.deepEqual(plano(a.estadoAccion(true, otro)), { habilitada: false, motivo: MOTIVO });
    assert.deepEqual(plano(a.estadoAccion(true, { ambienteCoincide: true, motivoAmbiente: null })), { habilitada: true, motivo: null });
});

test('las plantillas que cargan accion-envio-hacienda.js cargan después ambiente-comprobante.js, antes de facturas.js', () => {
    const dir = path.join(raiz, 'src/main/resources/templates/modules/facturacion');
    const plantillas = ['notas-credito.html', 'notas-credito-detalle.html', 'facturas.html',
        'pagos/listar.html', 'pagos/detalle.html', 'pagos/conciliacion.html'];
    for (const p of plantillas) {
        const html = fs.readFileSync(path.join(dir, p), 'utf8');
        const accion = html.indexOf('js/accion-envio-hacienda.js');
        const ambiente = html.indexOf('js/ambiente-comprobante.js');
        assert.ok(accion >= 0 && ambiente > accion, p);
    }
    const comprobantes = fs.readFileSync(path.join(dir, 'electronica/comprobantes.html'), 'utf8');
    assert.ok(comprobantes.includes('js/ambiente-comprobante.js'));
});

// ---- listado de notas (nota-credito.js filaNota) ----

function documentoFalso() {
    function crear(tag) {
        const e = {
            tag, hijos: [], attrs: {}, className: '', listeners: {}, _texto: '',
            appendChild(h) { this.hijos.push(h); return h; },
            setAttribute(k, v) { this.attrs[k] = String(v); },
            addEventListener(ev, fn) { this.listeners[ev] = fn; },
            get textContent() { return this._texto; },
            set textContent(v) { this._texto = String(v); }
        };
        for (const prop of ['innerHTML', 'outerHTML', 'insertAdjacentHTML']) {
            Object.defineProperty(e, prop, { set() { throw new Error(prop + ' asignado'); }, get() { return ''; } });
        }
        return e;
    }
    return { createElement: crear };
}

function cargarNotas() {
    const ctx = { window: {}, URLSearchParams };
    vm.createContext(ctx);
    vm.runInContext(fs.readFileSync(path.join(raiz,
        'src/main/resources/static/modules/facturacion/js/nota-credito.js'), 'utf8'), ctx);
    vm.runInContext(CODIGO, ctx);
    return ctx.window.NotaCredito;
}

const NOTA = { id: 5, numero: 'NC-5', idFactura: 9, total: 1, moneda: 'CRC', estadoComprobante: 'ENVIADO',
    consultableEnHacienda: true, puedeReenviar: true };
const ACCIONES = { verFactura() {}, consultar() {}, reenviar() {} };

function textoDe(e) {
    return e._texto + e.hijos.map(textoDe).join('|');
}

test('listado de notas: muestra el ambiente y, si no coincide, deshabilita Reenviar y Consultar con el motivo fijo', () => {
    const n = cargarNotas();
    const fila = n.filaNota(documentoFalso(), { ...NOTA, ambiente: 'SANDBOX', ambienteCoincide: false, motivoAmbiente: MOTIVO },
        ACCIONES);
    assert.ok(textoDe(fila.hijos[7]).includes('Sandbox'));
    const botones = fila.hijos[8].hijos[0].hijos;
    const consultar = botones.find((b) => b._texto === 'Consultar estado');
    const reenviar = botones.find((b) => b._texto === 'Reenviar');
    for (const b of [consultar, reenviar]) {
        assert.equal(b.attrs.disabled, 'disabled');
        assert.equal(b.attrs.title, MOTIVO);
        assert.equal(b.listeners.click, undefined);
    }
    assert.ok(botones.some((b) => b._texto === MOTIVO));
});

test('listado de notas: mismo ambiente → acciones habilitadas con su manejador y sin motivo', () => {
    const n = cargarNotas();
    const fila = n.filaNota(documentoFalso(), { ...NOTA, ambiente: 'PRODUCCION', ambienteCoincide: true, motivoAmbiente: null },
        ACCIONES);
    assert.ok(textoDe(fila.hijos[7]).includes('Producción'));
    const botones = fila.hijos[8].hijos[0].hijos;
    for (const texto of ['Consultar estado', 'Reenviar']) {
        const b = botones.find((x) => x._texto === texto);
        assert.equal(b.attrs.disabled, undefined);
        assert.equal(typeof b.listeners.click, 'function');
    }
    assert.ok(!botones.some((b) => b._texto === MOTIVO));
});

test('el motivo hostil del servidor entra por textContent, nunca como HTML', () => {
    const n = cargarNotas();
    const mal = '<img src=x onerror=alert(1)>';
    const fila = n.filaNota(documentoFalso(), { ...NOTA, ambiente: 'SANDBOX', ambienteCoincide: false, motivoAmbiente: mal },
        ACCIONES);
    assert.ok(textoDe(fila).includes(mal));
});

test('facturas.js y comprobantes.html pintan el motivo escapado y no reintroducen HTML con datos sin escapar', () => {
    const facturas = fs.readFileSync(path.join(raiz,
        'src/main/resources/static/modules/facturacion/js/facturas.js'), 'utf8');
    assert.ok(facturas.includes('escapeHtml(data.motivoAmbienteComprobante)'));
    assert.ok(facturas.includes('botonDes.textContent'));
    const comprobantes = fs.readFileSync(path.join(raiz,
        'src/main/resources/templates/modules/facturacion/electronica/comprobantes.html'), 'utf8');
    assert.ok(comprobantes.includes('escapeHtml(accion.motivo)'));
    assert.ok(comprobantes.includes('escapeHtml(comp.motivoAmbiente)'));
});
