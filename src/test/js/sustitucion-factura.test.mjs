// T-039 criterio 3: el botón «Emitir factura sustituta» solo cuando el servidor lo indica; textos y enlaces.
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CODIGO = fs.readFileSync(path.join(raiz,
    'src/main/resources/static/modules/facturacion/js/sustitucion-factura.js'), 'utf8');

function cargar() {
    const ctx = { window: {} };
    vm.createContext(ctx);
    vm.runInContext(CODIGO, ctx);
    return ctx.window.SustitucionFactura;
}

test('el botón aparece solo con puedeEmitirSustituta === true', () => {
    const s = cargar();
    assert.equal(s.puedeEmitir({ puedeEmitirSustituta: true }), true);
    for (const d of [{ puedeEmitirSustituta: false }, {}, null, undefined, { puedeEmitirSustituta: 'true' }]) {
        assert.equal(s.puedeEmitir(d), false);
    }
});

test('textos «Sustituye a» y «Sustituida por»', () => {
    const s = cargar();
    assert.equal(s.textoSustituye({ idFactura: 1, numeroFactura: 'FA01-00031', numeroNota: '00100001030000000007' }),
        'Sustituye a FA01-00031 (anulada por 00100001030000000007)');
    assert.equal(s.textoSustituye({ idFactura: 1, numeroFactura: 'FA01-00031', numeroNota: null }),
        'Sustituye a FA01-00031');
    assert.equal(s.textoSustituye(null), '');
    assert.equal(s.textoSustituidaPor({ idFactura: 2, numeroFactura: 'FA01-00032' }), 'Sustituida por FA01-00032');
    assert.equal(s.textoSustituidaPor(undefined), '');
});

test('cuerpo del pedido: razón opcional', () => {
    const s = cargar();
    assert.equal(JSON.stringify(s.cuerpoPedido('  Error de precio ')), '{"razon":"Error de precio"}');
    assert.equal(JSON.stringify(s.cuerpoPedido('   ')), '{}');
    assert.equal(JSON.stringify(s.cuerpoPedido(undefined)), '{}');
});

test('mensaje de error: el del servidor o uno fijo', () => {
    const s = cargar();
    assert.equal(s.mensajeDeError({ message: 'La factura ya tiene una sustituta: FA01-00040' }),
        'La factura ya tiene una sustituta: FA01-00040');
    assert.equal(s.mensajeDeError(null), 'No se pudo crear la factura sustituta');
});

test('las pantallas que cargan facturas.js cargan antes sustitucion-factura.js', () => {
    for (const f of ['modules/facturacion/facturas.html', 'shared/components/facturas-modal.html',
        'modules/facturacion/pagos/listar.html', 'modules/facturacion/pagos/conciliacion.html',
        'modules/facturacion/pagos/detalle.html']) {
        const html = fs.readFileSync(path.join(raiz, 'src/main/resources/templates', f), 'utf8');
        const a = html.indexOf('js/sustitucion-factura.js');
        const b = html.indexOf('js/facturas.js');
        assert.ok(a > 0 && a < b, f);
    }
});

test('T-046 fecha contable de la sustituta: dd/mm/aaaa sin desfase de zona; «-» si falta', () => {
    const s = cargar();
    assert.equal(s.textoFechaContable('2026-11-19'), '19/11/2026');
    assert.equal(s.textoFechaContable(null), '-');
});
