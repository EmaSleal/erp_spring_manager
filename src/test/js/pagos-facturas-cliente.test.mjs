// T-031 (P-040): cargarFacturasCliente de modules/facturacion/js/pagos.js.
//
// Dos cambios consecutivos de cliente con respuestas fuera de orden: cada factura debe aparecer una sola vez
// y solo con las facturas del último cliente. Se carga pagos.js en un contexto vm con un DOM mínimo simulado.
//
// Cómo se corre (Node 18 o superior): node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const PAGOS_JS = fs.readFileSync(path.join(raiz, 'src/main/resources/static/modules/facturacion/js/pagos.js'), 'utf8');
const FORM_HTML = fs.readFileSync(path.join(raiz, 'src/main/resources/templates/modules/facturacion/pagos/form.html'), 'utf8');

function pagina() {
    const peticiones = [];
    const facturaSelect = {
        _opciones: [],
        set innerHTML(v) { this._opciones = [{ value: '', textContent: 'Sin asignar (Adelanto)' }]; },
        appendChild(o) { this._opciones.push(o); },
        addEventListener() {},
    };
    const clienteSelect = { value: '', addEventListener() {} };
    const elementos = { idCliente: clienteSelect, idFactura: facturaSelect };
    const ctx = {
        console: { log() {}, error() {}, warn() {}, info() {} },
        document: {
            getElementById: (id) => elementos[id] ?? null,
            querySelector: () => null,
            createElement: () => ({ dataset: {} }),
            addEventListener() {},
        },
        window: { location: { search: '' } },
        formatNumber: (n) => String(n),
        showToast() {},
        httpGet: (url) => new Promise((resolve) => peticiones.push({ url, resolve })),
    };
    ctx.window.document = ctx.document;
    vm.createContext(ctx);
    vm.runInContext(PAGOS_JS, ctx);
    return { ctx, clienteSelect, facturaSelect, peticiones };
}

const textos = (select) => select._opciones.slice(1).map((o) => o.textContent.split(' - ')[0]);
const factura = (id, numero) => ({ idFactura: id, numeroFactura: numero, totalFactura: 10, saldoPendiente: 5, monedaFE: 'CRC' });

test('dos cambios seguidos de cliente: cada factura aparece una sola vez', async () => {
    const { ctx, clienteSelect, facturaSelect, peticiones } = pagina();

    clienteSelect.value = '1';
    const p1 = vm.runInContext('cargarFacturasCliente()', ctx);
    clienteSelect.value = '2';
    const p2 = vm.runInContext('cargarFacturasCliente()', ctx);
    assert.equal(peticiones.length, 2);

    // La respuesta de la llamada más reciente llega primero y la antigua después
    peticiones[1].resolve([factura(20, 'F-20')]);
    await p2;
    peticiones[0].resolve([factura(10, 'F-10'), factura(11, 'F-11')]);
    await p1;

    assert.deepEqual(textos(facturaSelect), ['F-20']);
});

test('respuestas en orden: solo quedan las facturas del último cliente, sin duplicados', async () => {
    const { ctx, clienteSelect, facturaSelect, peticiones } = pagina();

    clienteSelect.value = '1';
    const p1 = vm.runInContext('cargarFacturasCliente()', ctx);
    clienteSelect.value = '2';
    const p2 = vm.runInContext('cargarFacturasCliente()', ctx);

    peticiones[0].resolve([factura(10, 'F-10')]);
    await p1;
    peticiones[1].resolve([factura(20, 'F-20'), factura(21, 'F-21')]);
    await p2;

    assert.deepEqual(textos(facturaSelect), ['F-20', 'F-21']);
});

test('el select idCliente de pagos/form.html no declara onchange (el único handler es el de pagos.js)', () => {
    const select = /<select[^>]*id="idCliente"[^>]*>/s.exec(FORM_HTML);
    assert.ok(select, 'no se encontró el select idCliente');
    assert.doesNotMatch(select[0], /onchange/);
    assert.match(PAGOS_JS, /idClienteSelect\.addEventListener\('change', cargarFacturasCliente\)/);
});
