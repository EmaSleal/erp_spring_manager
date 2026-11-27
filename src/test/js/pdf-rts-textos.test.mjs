// T-059 criterio 9 (ADR-078): validación del título y la leyenda del PDF del RTS y vista previa en texto, solo con
// textContent (nunca innerHTML). El servidor es la autoridad; el JS refleja sus reglas para avisar antes de enviar.
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const RUTA_TEXTOS = 'src/main/resources/static/modules/configuracion/js/pdf-rts-textos.js';
const RUTA_FACTURACION = 'src/main/resources/static/modules/configuracion/js/configuracion-facturacion.js';
const CODIGO_TEXTOS = fs.readFileSync(path.join(raiz, RUTA_TEXTOS), 'utf8');
const CODIGO_FACTURACION = fs.readFileSync(path.join(raiz, RUTA_FACTURACION), 'utf8');

const MSG_OBLIGATORIO = 'El título del comprobante del régimen simplificado es obligatorio';
const MSG_LARGO = 'El título del comprobante del régimen simplificado no puede exceder 120 caracteres';
const MSG_SIN_FACTURA = 'El título del comprobante del régimen simplificado debe contener la palabra «Factura»';
const MSG_LEYENDA = 'La leyenda del comprobante del régimen simplificado no puede exceder 300 caracteres';

/** Elemento falso: innerHTML lanza si se toca; textContent y value son simples propiedades. */
function elemento(id) {
    const el = {
        id, value: '', textContent: '', hidden: false, checked: false, validez: '', listeners: {},
        addEventListener(ev, fn) { (el.listeners[ev] ||= []).push(fn); },
        setCustomValidity(m) { el.validez = m; },
        disparar(ev) { (el.listeners[ev] || []).forEach((f) => f({ target: el })); }
    };
    Object.defineProperty(el, 'innerHTML', { set() { throw new Error('innerHTML no se debe usar'); }, get() { throw new Error('innerHTML no se debe usar'); } });
    return el;
}

function documentoFalso() {
    const els = {};
    const doc = {
        getElementById: (id) => (els[id] ||= elemento(id)),
        addEventListener() {},
        els
    };
    return doc;
}

function cargarTextos() {
    const ctx = { window: {}, document: documentoFalso() };
    ctx.window.document = ctx.document;
    vm.createContext(ctx);
    vm.runInContext(CODIGO_TEXTOS, ctx);
    return ctx;
}

test('validarTitulo: obligatorio, 1 a 120 caracteres recortados y con la palabra «Factura»', () => {
    const u = cargarTextos().window.PdfRtsTextos;
    for (const v of ['', '   ', null, undefined, '\t\n']) {
        assert.equal(u.validarTitulo(v), MSG_OBLIGATORIO, String(v));
    }
    assert.equal(u.validarTitulo('Factura — Régimen de Tributación Simplificada'), null);
    assert.equal(u.validarTitulo('  Factura  '), null);
    const t120 = 'Factura ' + 'x'.repeat(112);
    assert.equal(u.validarTitulo(`  ${t120}  `), null);
    assert.equal(u.validarTitulo(t120 + 'x'), MSG_LARGO);
});

test('validarTitulo: «Factura» sin distinguir mayúsculas ni tildes, como palabra completa', () => {
    const u = cargarTextos().window.PdfRtsTextos;
    for (const ok of ['factura', 'FACTURA', 'Fáctura', 'FÁCTURA de venta', 'Mi Factura RTS', 'Una factura.', '(Factura)', 'Fáctura']) {
        assert.equal(u.validarTitulo(ok), null, ok);
    }
    for (const mal of ['Comprobante de venta', 'Facturación', 'Facturas', 'Prefactura', 'Fac tura', 'Recibo']) {
        assert.equal(u.validarTitulo(mal), MSG_SIN_FACTURA, mal);
    }
});

test('validarLeyenda: opcional, hasta 300 caracteres recortados', () => {
    const u = cargarTextos().window.PdfRtsTextos;
    for (const v of ['', '   ', null, undefined, 'No cobra IVA.', ` ${'a'.repeat(300)} `]) {
        assert.equal(u.validarLeyenda(v), null, String(v));
    }
    assert.equal(u.validarLeyenda('a'.repeat(301)), MSG_LEYENDA);
});

test('vista previa: título y leyenda se pintan con textContent; la leyenda vacía se oculta', () => {
    const ctx = cargarTextos();
    const u = ctx.window.PdfRtsTextos;
    u.pintarVistaPrevia(ctx.document, '  Factura RTS  ', '  No cobra IVA.  ');
    assert.equal(ctx.document.els['pdf-rts-prev-titulo'].textContent, 'Factura RTS');
    assert.equal(ctx.document.els['pdf-rts-prev-leyenda'].textContent, 'No cobra IVA.');
    assert.equal(ctx.document.els['pdf-rts-prev-leyenda'].hidden, false);

    u.pintarVistaPrevia(ctx.document, 'Factura RTS', '   ');
    assert.equal(ctx.document.els['pdf-rts-prev-leyenda'].textContent, '');
    assert.equal(ctx.document.els['pdf-rts-prev-leyenda'].hidden, true);
});

test('vista previa: un texto hostil queda como texto (no se interpreta; innerHTML lanza si se toca)', () => {
    const ctx = cargarTextos();
    const hostil = '<img src=x onerror=alert(1)> & "comillas" \'simples\' <script>boom()</script>';
    ctx.window.PdfRtsTextos.pintarVistaPrevia(ctx.document, 'Factura ' + hostil, hostil);
    assert.equal(ctx.document.els['pdf-rts-prev-titulo'].textContent, 'Factura ' + hostil);
    assert.equal(ctx.document.els['pdf-rts-prev-leyenda'].textContent, hostil);
});

test('enlazar: al escribir se actualiza la vista previa y el aviso del título (textContent + validez del campo)', () => {
    const ctx = cargarTextos();
    const u = ctx.window.PdfRtsTextos;
    u.enlazar(ctx.document);
    const titulo = ctx.document.els['pdfRtsTitulo'];
    const leyenda = ctx.document.els['pdfRtsLeyenda'];

    titulo.value = 'Recibo';
    titulo.disparar('input');
    assert.equal(ctx.document.els['pdf-rts-prev-titulo'].textContent, 'Recibo');
    assert.equal(ctx.document.els['pdf-rts-titulo-error'].textContent, MSG_SIN_FACTURA);
    assert.equal(titulo.validez, MSG_SIN_FACTURA);

    titulo.value = 'Factura de venta';
    leyenda.value = 'Sin IVA';
    leyenda.disparar('input');
    assert.equal(ctx.document.els['pdf-rts-prev-titulo'].textContent, 'Factura de venta');
    assert.equal(ctx.document.els['pdf-rts-prev-leyenda'].textContent, 'Sin IVA');
    assert.equal(ctx.document.els['pdf-rts-titulo-error'].textContent, '');
    assert.equal(titulo.validez, '');
});

test('ningún JS de la pestaña usa innerHTML', () => {
    assert.ok(!/innerHTML/.test(CODIGO_TEXTOS), 'pdf-rts-textos.js no debe usar innerHTML');
    assert.ok(!/innerHTML/.test(CODIGO_FACTURACION), 'configuracion-facturacion.js no debe usar innerHTML');
});

function cargarFacturacion(respuestaGet, llamadas) {
    const ctx = cargarTextos();
    ctx.Configuracion = {
        get: async () => respuestaGet,
        put: async (url, datos) => { llamadas.push(['put', url, datos]); return { success: true, message: 'ok' }; },
        post: async (url, datos) => { llamadas.push(['post', url, datos]); return { success: true, message: 'ok' }; },
        validarFormulario: () => true,
        mostrarAlertaEn: (cont, tipo, msg) => llamadas.push(['alerta', tipo, msg])
    };
    ctx.setTimeout = () => 0;
    ctx.console = { log() {}, error() {} };
    vm.runInContext(CODIGO_FACTURACION, ctx);
    return ctx;
}

test('configuracion-facturacion: carga los dos campos y los envía en el guardado, recortados', async () => {
    const llamadas = [];
    const ctx = cargarFacturacion({
        success: true,
        data: { id: 1, serieFactura: 'FA01', pdfRtsTitulo: 'Factura RTS', pdfRtsLeyenda: 'No cobra IVA.' }
    }, llamadas);
    const mod = vm.runInContext('ConfiguracionFacturacion', ctx);
    await mod.cargarConfiguracion();
    assert.equal(ctx.document.els['pdfRtsTitulo'].value, 'Factura RTS');
    assert.equal(ctx.document.els['pdfRtsLeyenda'].value, 'No cobra IVA.');
    assert.equal(ctx.document.els['pdf-rts-prev-titulo'].textContent, 'Factura RTS');

    ctx.document.els['pdfRtsTitulo'].value = '  Factura de venta  ';
    ctx.document.els['pdfRtsLeyenda'].value = '   ';
    await mod.guardarConfiguracion();
    const put = llamadas.find((l) => l[0] === 'put');
    assert.ok(put, 'debe llamar a PUT');
    assert.equal(put[2].pdfRtsTitulo, 'Factura de venta');
    assert.equal(put[2].pdfRtsLeyenda, null);
});

test('configuracion-facturacion: un título sin «Factura» no se envía y se avisa con el mensaje fijo', async () => {
    const llamadas = [];
    const ctx = cargarFacturacion({ success: true, data: { id: 1 } }, llamadas);
    const mod = vm.runInContext('ConfiguracionFacturacion', ctx);
    ctx.document.els['idConfiguracionFacturacion'] = Object.assign(elemento('idConfiguracionFacturacion'), { value: '1' });
    ctx.document.els['pdfRtsTitulo'] = Object.assign(elemento('pdfRtsTitulo'), { value: 'Recibo' });
    await mod.guardarConfiguracion();
    assert.ok(!llamadas.some((l) => l[0] === 'put' || l[0] === 'post'), 'no debe enviar');
    assert.ok(llamadas.some((l) => l[0] === 'alerta' && l[2] === MSG_SIN_FACTURA));
});
