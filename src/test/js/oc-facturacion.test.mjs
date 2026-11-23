// T-053 (ADR-066 §4, ADR-067): detalle de la orden de compra para facturar. Facturado, pendiente y exceso, CxP de la
// orden, advertencia de exceso sin bloquear, líneas de la factura con la cuenta propuesta (o sin ella) y pintado solo con
// textContent.
//   node --test src/test/js/*.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CXP = path.join(raiz, 'src/main/resources/static/modules/contabilidad/js/cxp-lineas.js');
const OC = path.join(raiz, 'src/main/resources/static/modules/proveedores/js/oc-facturacion.js');
const PLANTILLA = path.join(raiz, 'src/main/resources/templates/modules/proveedores/ordenes-compra.html');
const MAL = '<img src=x onerror=alert(1)>';

function cargar() {
    const ctx = { window: {}, console, Number, Math, Date, String, Array, Object };
    vm.createContext(ctx);
    vm.runInContext(fs.readFileSync(CXP, 'utf8'), ctx);
    vm.runInContext(fs.readFileSync(OC, 'utf8'), ctx);
    return ctx.window.OcFacturacion;
}

function crear(tag) {
    const e = {
        tag, hijos: [], attrs: {}, className: '', _texto: '', value: '', style: {},
        appendChild(h) { this.hijos.push(h); return h; },
        setAttribute(k, v) { this.attrs[k] = String(v); },
        getAttribute(k) { return this.attrs[k] ?? null; },
        addEventListener() {},
        replaceChildren() { this.hijos = []; this._texto = ''; },
        get textContent() { return this._texto; },
        set textContent(v) { this._texto = String(v); this.hijos = []; }
    };
    for (const prop of ['innerHTML', 'outerHTML', 'insertAdjacentHTML']) {
        Object.defineProperty(e, prop, { set() { throw new Error(prop + ' asignado'); }, get() { return ''; } });
    }
    return e;
}
const textoDe = (e) => e._texto + '|' + e.hijos.map(textoDe).join('|');
const recorrer = (e, f) => { f(e); e.hijos.forEach((h) => recorrer(h, f)); };
const documento = () => ({ createElement: crear });

function dto(extra = {}) {
    return {
        ordenCompraId: 9, numero: 'OC-2026-00009', proveedorId: 3, proveedorNombre: 'Distribuidora S.A.',
        estado: 'RECIBIDA_PARCIAL', admiteFacturar: true, moneda: 'CRC',
        total: 113000, facturado: 56500, pendiente: 56500, exceso: 0,
        cuentaPropuestaId: 41, cuentaPropuestaCodigo: '5.3.01.001', cuentaPropuestaNombre: 'Compras mercadería',
        detalles: [
            { id: 1, descripcion: 'Resina A', cantidad: 100, precioUnitario: 500, total: 50000 },
            { id: 2, descripcion: 'Resina B', cantidad: 10, precioUnitario: 5000, total: 50000 }
        ],
        cuentas: [
            { id: 70, numero: 'CPP-2026-00070', numeroComprobanteProveedor: 'FE-1', fechaComprobante: '2026-10-20',
              monto: 56500, estado: 'PENDIENTE', estadoAceptacion: 'PENDIENTE' }
        ],
        ...extra
    };
}

test('lineasDesdeOc: una línea por detalle con base = cantidad × precio, 13 %, pleno y la cuenta propuesta', () => {
    const o = cargar();
    const lineas = o.lineasDesdeOc(dto().detalles, 41);
    assert.equal(lineas.length, 2);
    assert.equal(lineas[0].cuentaId, 41);
    assert.equal(lineas[0].descripcion, 'Resina A');
    assert.equal(lineas[0].base, 50000);
    assert.equal(lineas[0].iva, 6500);
    assert.equal(lineas[0].codigoTarifa, '08');
    assert.equal(lineas[0].tratamiento, 'PLENO');
    assert.equal(lineas[1].base, 50000);
});

test('lineasDesdeOc: sin cuenta propuesta la cuenta queda vacía (no se propone ninguna) y las líneas se arman igual', () => {
    const o = cargar();
    for (const propuesta of [null, undefined, '']) {
        const lineas = o.lineasDesdeOc(dto().detalles, propuesta);
        assert.equal(lineas.length, 2);
        assert.equal(lineas[0].cuentaId, '');
        assert.equal(lineas[0].base, 50000);
    }
});

test('lineasDesdeOc: sin detalles, una línea por defecto (con la cuenta propuesta si la hay)', () => {
    const o = cargar();
    const lineas = o.lineasDesdeOc([], 41);
    assert.equal(lineas.length, 1);
    assert.equal(lineas[0].cuentaId, 41);
    assert.equal(lineas[0].base, '');
    assert.equal(o.lineasDesdeOc(null, null)[0].cuentaId, '');
});

test('advertenciaExceso: texto fijo con lo facturado, el total y la diferencia; null sin exceso', () => {
    const o = cargar();
    assert.equal(o.advertenciaExceso(dto()), null);
    const d = dto({ facturado: 124300, pendiente: 0, exceso: 11300 });
    assert.equal(o.advertenciaExceso(d),
        'Lo facturado (124300.00) supera el total de la orden de compra OC-2026-00009 (113000.00) por 11300.00');
});

test('puedeFacturar: solo con la orden en un estado que admite facturar y con permiso', () => {
    const o = cargar();
    assert.equal(o.puedeFacturar(dto(), true), true);
    assert.equal(o.puedeFacturar(dto(), false), false);
    assert.equal(o.puedeFacturar(dto({ admiteFacturar: false, estado: 'APROBADA' }), true), false);
    assert.equal(o.puedeFacturar(null, true), false);
});

test('renderDetalle: total, facturado, pendiente, exceso, la CxP con comprobante, fecha, total y estado', () => {
    const o = cargar();
    const cont = crear('div');
    o.renderDetalle(documento(), cont, dto());
    const t = textoDe(cont);
    for (const esperado of ['OC-2026-00009', 'Distribuidora S.A.', '113000.00', '56500.00', 'Pendiente de facturar', 'Exceso',
        'CPP-2026-00070', 'FE-1', '2026-10-20', 'PENDIENTE', 'Resina A']) {
        assert.ok(t.includes(esperado), 'falta ' + esperado);
    }
    assert.ok(!t.includes('supera el total'), 'sin exceso no hay advertencia');
});

test('renderDetalle: con exceso muestra la advertencia (alerta, sin bloquear nada)', () => {
    const o = cargar();
    const cont = crear('div');
    o.renderDetalle(documento(), cont, dto({ facturado: 124300, pendiente: 0, exceso: 11300 }));
    const alertas = [];
    recorrer(cont, (e) => { if (String(e.className).includes('alert-warning')) alertas.push(e._texto); });
    assert.equal(alertas.length, 1);
    assert.ok(alertas[0].includes('supera el total de la orden de compra OC-2026-00009'));
    assert.ok(alertas[0].includes('11300.00'));
});

test('renderDetalle: sin CxP indica que aún no hay facturas', () => {
    const o = cargar();
    const cont = crear('div');
    o.renderDetalle(documento(), cont, dto({ cuentas: [], facturado: 0, pendiente: 113000 }));
    assert.ok(textoDe(cont).includes('aún no tiene facturas'));
});

test('renderAdvertencias: pinta cada advertencia de la respuesta del alta como alerta', () => {
    const o = cargar();
    const cont = crear('div');
    o.renderAdvertencias(documento(), cont, ['Primera', 'Segunda']);
    assert.equal(cont.hijos.length, 2);
    assert.equal(cont.hijos[1]._texto, 'Segunda');
    o.renderAdvertencias(documento(), cont, []);
    assert.equal(cont.hijos.length, 0);
});

test('un texto malicioso del servidor (descripción, comprobante, proveedor, estado) solo se pinta con textContent', () => {
    const o = cargar();
    const d = dto({
        proveedorNombre: MAL,
        detalles: [{ id: 1, descripcion: MAL, cantidad: 1, precioUnitario: 1, total: 1 }],
        cuentas: [{ id: 1, numero: MAL, numeroComprobanteProveedor: MAL, fechaComprobante: MAL, monto: 1, estado: MAL, estadoAceptacion: MAL }]
    });
    const cont = crear('div');
    // cualquier asignación a innerHTML/insertAdjacentHTML lanza: si no lanza, no se usó
    o.renderDetalle(documento(), cont, d);
    assert.ok(textoDe(cont).includes(MAL));
    const filas = crear('tbody');
    o.renderFilasRecepcion(documento(), filas, [{ id: 5, descripcion: MAL, cantidad: 3, cantidadRecibida: 1, porRecibir: 2 }]);
    assert.ok(textoDe(filas).includes(MAL));
    assert.equal(filas.hijos[0].getAttribute('data-detalle-id'), '5');
    const inputs = [];
    recorrer(filas, (e) => { if (e.tag === 'input') inputs.push(e); });
    assert.equal(inputs.length, 3);
    assert.equal(inputs[0].value, 2);
});

test('renderFilasRecepcion: sin líneas muestra el aviso', () => {
    const o = cargar();
    const filas = crear('tbody');
    o.renderFilasRecepcion(documento(), filas, []);
    assert.ok(textoDe(filas).includes('no tiene líneas'));
});

test('la plantilla de órdenes de compra ya no interpola la descripción en innerHTML ni conserva el botón de generar CPP', () => {
    const html = fs.readFileSync(PLANTILLA, 'utf8');
    assert.ok(!/innerHTML\s*=\s*`[^`]*\$\{d\.descripcion\}/.test(html), 'la fila de recepción interpola descripcion');
    assert.ok(!/\$\{d\.descripcion\}/.test(html));
    assert.ok(!html.includes('btn-generar-cpp'));
    assert.ok(!html.includes('generar-cpp'));
    assert.ok(html.includes('/modules/proveedores/js/oc-facturacion.js'));
    assert.ok(html.includes('/modules/contabilidad/js/cxp-lineas.js'));
    assert.ok(html.includes('Registrar factura del proveedor'));
});
