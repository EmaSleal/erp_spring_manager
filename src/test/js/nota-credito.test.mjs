// T-042 criterio 14: botón «Emitir nota de crédito» por banderas, contador y validación de la razón, mensajes y
// pintado con textContent.   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const JS = path.join(raiz, 'src/main/resources/static/modules/facturacion/js');
const TEMPLATES = path.join(raiz, 'src/main/resources/templates');
const ARCHIVOS_NUEVOS = ['nota-credito.js', 'nota-credito-modal.js', 'notas-credito.js', 'nota-credito-detalle.js'];

function cargar(extra = {}) {
    const ctx = { window: {}, URLSearchParams, ...extra };
    vm.createContext(ctx);
    vm.runInContext(fs.readFileSync(path.join(JS, 'nota-credito.js'), 'utf8'), ctx);
    return ctx.window.NotaCredito;
}

// ---- botón ----

test('botón habilitado solo con puedeEmitirNotaCredito === true', () => {
    const n = cargar();
    assert.deepEqual({ ...n.estadoBoton({ puedeEmitirNotaCredito: true }) },
        { visible: true, habilitado: true, motivo: null });
});

test('botón deshabilitado con el motivo cuando el servidor lo informa', () => {
    const n = cargar();
    const motivo = 'La nota de crédito sobre facturas con pagos llega en una próxima versión';
    assert.equal(n.MOTIVO_PAGOS, motivo);
    assert.deepEqual({ ...n.estadoBoton({ puedeEmitirNotaCredito: false, motivoNotaCreditoNoDisponible: motivo }) },
        { visible: true, habilitado: false, motivo });
});

test('sin bandera ni motivo no hay botón; los roles no cuentan', () => {
    const n = cargar();
    for (const d of [{}, null, undefined, { puedeEmitirNotaCredito: false }, { puedeEmitirNotaCredito: 'true' },
        { puedeEmitirNotaCredito: false, motivoNotaCreditoNoDisponible: '  ' },
        { puedeEmitirNotaCredito: false, rol: 'ADMIN', userRole: 'ADMIN', permisos: ['FACTURA_ANULAR'] }]) {
        assert.equal(n.estadoBoton(d).visible, false);
        assert.equal(n.estadoBoton(d).habilitado, false);
    }
    // con la bandera, el rol tampoco la quita
    assert.equal(n.estadoBoton({ puedeEmitirNotaCredito: true, userRole: 'VISUALIZADOR' }).habilitado, true);
});

// ---- razón ----

test('contador de la razón', () => {
    const n = cargar();
    assert.equal(n.contadorRazon(''), '0/180');
    assert.equal(n.contadorRazon('Error de precio'), '15/180');
    assert.equal(n.contadorRazon(undefined), '0/180');
});

test('validación de la razón: 3 a 180 con recorte de espacios', () => {
    const n = cargar();
    assert.notEqual(n.validarRazon('ab'), null);
    assert.equal(n.validarRazon('abc'), null);
    assert.equal(n.validarRazon('a'.repeat(180)), null);
    assert.notEqual(n.validarRazon('a'.repeat(181)), null);
    assert.notEqual(n.validarRazon('   ab   '), null);
    assert.equal(n.validarRazon('  abc  '), null);
    assert.notEqual(n.validarRazon(''), null);
    assert.notEqual(n.validarRazon(null), null);
    assert.equal(n.validarRazon('x'), n.ERROR_RAZON);
});

// ---- pedido, efecto y resumen ----

test('cuerpo del pedido y texto del efecto por código', () => {
    const n = cargar();
    assert.equal(JSON.stringify(n.cuerpoPedido(7, '13', '  Error material ')),
        '{"idFactura":7,"tipo":"CREDITO","codigoReferencia":"13","razon":"Error material"}');
    assert.match(n.textoEfecto('13'), /periodo de la factura original/);
    assert.match(n.textoEfecto('01'), /periodo de la fecha de la nota/);
    assert.equal(n.textoEfecto('99'), '');
    assert.equal(n.nombreCodigo('01'), 'Anula documento de referencia');
    assert.equal(n.nombreCodigo('13'), 'Anula documento de referencia por error material');
});

test('resumen: factura, cliente, monto, moneda y aviso fijo', () => {
    const n = cargar();
    const r = n.resumen({ numeroFactura: 'FA01-00031', cliente: { nombre: 'ACME' }, total: 1234567.5, monedaFE: 'USD' });
    assert.equal(r.factura, 'FA01-00031');
    assert.equal(r.cliente, 'ACME');
    assert.equal(r.monto, '$1,234,567.50');
    assert.equal(r.moneda, 'USD');
    assert.equal(r.aviso,
        'La factura quedará anulada cuando Hacienda acepte la nota; después podrá emitir la factura sustituta');
    assert.equal(n.resumen({ idFactura: 5, total: 10 }).moneda, 'CRC');
});

// ---- mensajes ----

test('mensaje de resultado: el del servidor o uno fijo', () => {
    const n = cargar();
    assert.equal(n.mensajeResultado({ message: 'Nota de crédito emitida. Recibido por Hacienda' }),
        'Nota de crédito emitida. Recibido por Hacienda');
    assert.equal(n.mensajeResultado({ message: '   ' }), 'Nota de crédito emitida.');
    assert.equal(n.mensajeResultado(null), 'Nota de crédito emitida.');
});

test('mensaje de error por estado HTTP', () => {
    const n = cargar();
    for (const s of [400, 404, 409, 422]) {
        assert.equal(n.mensajeDeError(s, { message: 'Texto del servidor' }), 'Texto del servidor');
        assert.ok(n.mensajeDeError(s, null).length > 10);
    }
    assert.notEqual(n.mensajeDeError(400, null), n.mensajeDeError(409, null));
    // 500 y otros: nunca el texto del servidor (podría ser técnico)
    assert.equal(n.mensajeDeError(500, { message: 'java.lang.NullPointerException' }),
        'No se pudo emitir la nota de crédito. Intente de nuevo');
    assert.equal(n.mensajeDeError(0, null), 'No se pudo emitir la nota de crédito. Intente de nuevo');
});

test('resultados de reenviar y consultar estado (mismos criterios que comprobantes)', () => {
    const n = cargar();
    assert.deepEqual({ ...n.resultadoReenvio(true, { success: true, message: 'Recibido' }) },
        { tipo: 'success', mensaje: 'Recibido' });
    assert.equal(n.resultadoReenvio(true, { success: false }).tipo, 'warning');
    assert.equal(n.resultadoReenvio(false, {}).tipo, 'error');
    assert.equal(n.resultadoReenvio(false, {}).mensaje, 'No se pudo reenviar el comprobante');
    assert.equal(n.resultadoConsulta(429, { success: false, message: 'Disponible desde 10:00' }).tipo, 'info');
    assert.equal(n.resultadoConsulta(409, { success: false }).tipo, 'warning');
    assert.equal(n.resultadoConsulta(502, {}).mensaje, 'No se pudo consultar el estado');
    assert.equal(n.resultadoConsulta(200, { success: true, message: 'ACEPTADO' }).exito, true);
});

test('query del listado: solo filtros con valor, página y tamaño', () => {
    const n = cargar();
    assert.equal(n.queryListado({ desde: '2026-01-01', hasta: '', cliente: ' ACME ', estado: 'APLICADA' }, 2, 20),
        'desde=2026-01-01&cliente=ACME&estado=APLICADA&page=2&size=20');
    assert.equal(n.queryListado({}, 0, 20), 'page=0&size=20');
    assert.equal(n.textoPagina({ pagina: 1, tamano: 20, totalElementos: 45, totalPaginas: 3 }),
        'Mostrando 21-40 de 45 (página 2 de 3)');
    assert.equal(n.textoPagina({ pagina: 0, tamano: 20, totalElementos: 0, totalPaginas: 0 }), 'Sin notas de crédito');
});

test('badge del comprobante: «Enviando…» para ENVIO_INCIERTO reciente', () => {
    const ctx = { window: {} };
    vm.createContext(ctx);
    vm.runInContext(fs.readFileSync(path.join(raiz,
        'src/main/resources/static/modules/facturacion/electronica/js/estado-envio.js'), 'utf8'), ctx);
    vm.runInContext(fs.readFileSync(path.join(JS, 'nota-credito.js'), 'utf8'), ctx);
    const n = ctx.window.NotaCredito;
    const ahora = Date.parse('2026-05-01T10:01:00');
    assert.equal(n.badgeComprobante('ENVIO_INCIERTO', '2026-05-01T10:00:30', ahora).texto, 'Enviando…');
    assert.equal(n.badgeComprobante('ENVIO_INCIERTO', '2026-05-01T09:00:00', ahora).texto, 'Envío incierto');
    assert.equal(n.badgeComprobante('ACEPTADO', null, ahora).texto, 'Aceptado');
    assert.equal(n.badgeNota('EN_TRAMITE').texto, 'En trámite');
});

// ---- textContent ----

/** Documento falso mínimo: registra textContent y falla si alguien asigna innerHTML/outerHTML. */
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

function textoDe(e) {
    return e._texto + e.hijos.map(textoDe).join('|');
}

test('la fila del listado pinta datos hostiles con textContent y nunca con innerHTML', () => {
    const n = cargar();
    const mal = '<img src=x onerror=alert(1)>';
    const doc = documentoFalso();
    const fila = n.filaNota(doc, {
        id: 5, numero: mal, idFactura: 9, numeroFactura: mal, clienteNombre: mal, codigoReferencia: '01',
        codigoNombre: mal, total: 100, moneda: 'CRC', fechaEmision: '2026-05-01T10:00:00', estado: mal,
        estadoComprobante: mal, puedeReenviar: true, consultableEnHacienda: true
    }, { verFactura() {}, consultar() {}, reenviar() {} });
    const todo = textoDe(fila);
    assert.ok(todo.includes(mal));
    assert.equal(fila.hijos[0].hijos[0]._texto, mal); // número
    assert.equal(fila.hijos[2]._texto, mal); // cliente
    const acciones = fila.hijos[8].hijos[0];
    const etiquetas = acciones.hijos.map((h) => h._texto);
    assert.deepEqual(etiquetas, ['Ver detalle', 'PDF', 'Consultar estado', 'Reenviar']);
    assert.equal(acciones.hijos[0].attrs.href, '/facturas/notas-credito/5');
    assert.equal(acciones.hijos[1].attrs.href, '/facturas/notas-credito/5/pdf');
});

test('las acciones Consultar y Reenviar solo aparecen con las banderas', () => {
    const n = cargar();
    const base = { id: 1, numero: 'NC-1', idFactura: 2, total: 1, moneda: 'CRC' };
    const doc = documentoFalso();
    const sin = n.filaNota(doc, base, { verFactura() {}, consultar() {}, reenviar() {} });
    assert.deepEqual(sin.hijos[8].hijos[0].hijos.map((h) => h._texto), ['Ver detalle', 'PDF']);
});

test('ningún JS nuevo asigna innerHTML/outerHTML ni usa insertAdjacentHTML ni document.write', () => {
    for (const f of ARCHIVOS_NUEVOS) {
        const codigo = fs.readFileSync(path.join(JS, f), 'utf8');
        assert.doesNotMatch(codigo, /\.(innerHTML|outerHTML)\s*[+\-]?=/, f);
        assert.doesNotMatch(codigo, /insertAdjacentHTML|document\.write/, f);
    }
});

// ---- orden de scripts ----

test('nota-credito.js carga antes de facturas.js (y sustitucion-factura.js también)', () => {
    for (const f of ['modules/facturacion/facturas.html', 'shared/components/facturas-modal.html',
        'modules/facturacion/pagos/listar.html', 'modules/facturacion/pagos/conciliacion.html',
        'modules/facturacion/pagos/detalle.html', 'modules/facturacion/notas-credito.html',
        'modules/facturacion/notas-credito-detalle.html']) {
        const html = fs.readFileSync(path.join(TEMPLATES, f), 'utf8');
        const nc = html.indexOf('js/nota-credito.js');
        const modal = html.indexOf('js/nota-credito-modal.js');
        const fac = html.indexOf('js/facturas.js');
        assert.ok(nc > 0 && nc < fac, f + ' nota-credito.js');
        assert.ok(modal > nc && modal < fac, f + ' nota-credito-modal.js');
    }
});
