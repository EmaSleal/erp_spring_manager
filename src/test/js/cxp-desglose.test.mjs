// T-052 (ADR-066 §1-2): desglose de IVA de la cuenta por pagar. Cálculo en vivo, validaciones (espejo de las del
// servidor), avisos y pintado solo con textContent.
//   node --test src/test/js/*.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const ARCHIVO = path.join(raiz, 'src/main/resources/static/modules/contabilidad/js/cxp-desglose.js');
const MAL = '<img src=x onerror=alert(1)>';

function cargar() {
    const ctx = { window: {}, console, Number, Math, Date, String, Array, Object };
    vm.createContext(ctx);
    vm.runInContext(fs.readFileSync(ARCHIVO, 'utf8'), ctx);
    return ctx.window.CxpDesglose;
}

function crear(tag) {
    const e = {
        tag, hijos: [], attrs: {}, className: '', listeners: {}, _texto: '', value: '', disabled: false, style: {},
        appendChild(h) { this.hijos.push(h); return h; },
        setAttribute(k, v) { this.attrs[k] = String(v); },
        getAttribute(k) { return this.attrs[k] ?? null; },
        addEventListener(ev, fn) { this.listeners[ev] = fn; },
        replaceChildren() { this.hijos = []; this._texto = ''; },
        get textContent() { return this._texto; },
        set textContent(v) { this._texto = String(v); this.hijos = []; }
    };
    for (const prop of ['innerHTML', 'outerHTML', 'insertAdjacentHTML']) {
        Object.defineProperty(e, prop, { set() { throw new Error(prop + ' asignado'); }, get() { return ''; } });
    }
    return e;
}
const textoDe = (e) => e._texto + e.hijos.map(textoDe).join('|');
const recorrer = (e, f) => { f(e); e.hijos.forEach((h) => recorrer(h, f)); };
const documento = () => ({ createElement: crear });

test('calcularFila: base × tarifa / 100 redondeado a 2 decimales (mitad hacia arriba)', () => {
    const d = cargar();
    assert.equal(d.calcularFila(100000, 13), 13000);
    assert.equal(d.calcularFila('100.50', 13), 13.07);   // 13.065 → 13.07
    assert.equal(d.calcularFila(0.05, 13), 0.01);        // 0.0065 → 0.01
    assert.equal(d.calcularFila('', 13), 0);
    assert.equal(d.calcularFila(1000, 0), 0);
    assert.equal(d.calcularFila(1000, 0.5), 5);
});

test('catálogo de tarifas: 13 % es la tarifa general («08») y trae las once del catálogo', () => {
    const d = cargar();
    assert.equal(d.TARIFAS.length, 11);
    assert.equal(d.TARIFAS.find((t) => t.codigo === '08').porcentaje, 13);
    assert.equal(d.TARIFAS.find((t) => t.codigo === '07').porcentaje, 8);
    assert.equal(d.TARIFAS.find((t) => t.codigo === '10').porcentaje, 0);
    assert.equal(d.porcentajeDe('04'), 4);
    assert.equal(d.porcentajeDe('99'), null);
});

test('filaPorDefecto: una fila al 13 %', () => {
    const d = cargar();
    const f = d.filaPorDefecto();
    assert.equal(f.codigoTarifa, '08');
    assert.equal(f.base, '');
});

test('totales: subtotal = Σ bases, impuesto = Σ IVA, total = subtotal + impuesto (sin error de coma flotante)', () => {
    const d = cargar();
    const t = d.totales([{ base: '0.10', iva: '0.01' }, { base: '0.20', iva: '0.03' }, { base: '', iva: '' }]);
    assert.equal(t.subtotal, 0.3);
    assert.equal(t.impuesto, 0.04);
    assert.equal(t.total, 0.34);
    assert.deepEqual({ ...d.totales([]) }, { subtotal: 0, impuesto: 0, total: 0 });
});

const OK = () => ({ numero: 'FE-1', clave: '', fecha: '2026-11-20', hoy: '2026-11-22',
    filas: [{ codigoTarifa: '08', base: '100000', iva: '13000' }], totalComprobante: '' });

test('validar: un comprobante completo no da errores ni avisos', () => {
    const d = cargar();
    const r = d.validar(OK());
    assert.deepEqual([...r.errores], []);
    assert.deepEqual([...r.advertencias], []);
});

test('validar: número obligatorio y de hasta 50 caracteres, fecha obligatoria y no futura', () => {
    const d = cargar();
    assert.match(d.validar({ ...OK(), numero: '  ' }).errores[0], /número del comprobante/);
    assert.match(d.validar({ ...OK(), numero: 'N'.repeat(51) }).errores[0], /50 caracteres/);
    assert.match(d.validar({ ...OK(), fecha: '' }).errores[0], /fecha de emisión/);
    assert.match(d.validar({ ...OK(), fecha: '2026-11-23' }).errores[0], /no puede ser futura/);
    assert.equal(d.validar({ ...OK(), fecha: '2026-11-22' }).errores.length, 0);
});

test('validar: la clave, si viene, debe tener 50 dígitos', () => {
    const d = cargar();
    assert.equal(d.validar({ ...OK(), clave: '1'.repeat(50) }).errores.length, 0);
    assert.match(d.validar({ ...OK(), clave: '123' }).errores[0], /50 dígitos/);
    assert.match(d.validar({ ...OK(), clave: '1'.repeat(49) + 'x' }).errores[0], /50 dígitos/);
});

test('validar: al menos una fila y cada fila con base e IVA', () => {
    const d = cargar();
    assert.match(d.validar({ ...OK(), filas: [] }).errores[0], /al menos una fila/);
    assert.match(d.validar({ ...OK(), filas: [{ codigoTarifa: '08', base: '', iva: '' }] }).errores[0], /base/);
    assert.match(d.validar({ ...OK(), filas: [{ codigoTarifa: '08', base: '-5', iva: '0' }] }).errores[0], /base/);
});

test('validar: IVA distinto de base × tarifa por más de 0,01 es un aviso; dentro de la tolerancia, no', () => {
    const d = cargar();
    const aviso = d.validar({ ...OK(), filas: [{ codigoTarifa: '08', base: '100000', iva: '12999' }] });
    assert.equal(aviso.errores.length, 0);
    assert.equal(aviso.advertencias.length, 1);
    assert.match(aviso.advertencias[0], /fila 1/);
    assert.equal(d.validar({ ...OK(), filas: [{ codigoTarifa: '08', base: '100000', iva: '13000.01' }] }).advertencias.length, 0);
});

test('validar: avisa si el total del comprobante no cuadra con la suma del desglose', () => {
    const d = cargar();
    assert.equal(d.validar({ ...OK(), totalComprobante: '113000' }).advertencias.length, 0);
    assert.equal(d.validar({ ...OK(), totalComprobante: '113000.01' }).advertencias.length, 0);
    const r = d.validar({ ...OK(), totalComprobante: '120000' });
    assert.equal(r.advertencias.length, 1);
    assert.match(r.advertencias[0], /no cuadra/);
});

test('armarPeticion: subtotal, impuesto y monto salen del desglose; la fecha de emisión es la del comprobante', () => {
    const d = cargar();
    const p = d.armarPeticion({ numero: ' FE-1 ', clave: '', fecha: '2026-11-20',
        filas: [{ codigoTarifa: '08', base: '60000', iva: '7800' }, { codigoTarifa: '07', base: '40000', iva: '3200' }] });
    assert.equal(p.numeroComprobanteProveedor, 'FE-1');
    assert.equal(p.claveComprobante, null);
    assert.equal(p.fechaComprobante, '2026-11-20');
    assert.equal(p.fechaEmision, '2026-11-20');
    assert.equal(p.subtotal, 100000);
    assert.equal(p.impuesto, 11000);
    assert.equal(p.monto, 111000);
    assert.deepEqual(p.desglose.map((x) => [x.codigoTarifa, x.base, x.iva]), [['08', 60000, 7800], ['07', 40000, 3200]]);
});

test('renderFila: pinta el selector de tarifa, base e IVA con textContent y avisa al cambiar o quitar', () => {
    const d = cargar();
    const llamadas = [];
    const fila = d.renderFila(documento(), { codigoTarifa: '08', base: '1000', iva: '130' }, 0,
        { cambio: (i, campo, valor) => llamadas.push(['cambio', i, campo, valor]), quitar: (i) => llamadas.push(['quitar', i]) });
    assert.equal(fila.tag, 'tr');
    const controles = [];
    recorrer(fila, (x) => { if (x.attrs['data-campo']) controles.push(x); });
    assert.deepEqual(controles.map((c) => c.attrs['data-campo']), ['codigoTarifa', 'base', 'iva']);
    assert.equal(controles[0].value, '08');
    const opciones = controles[0].hijos.map((o) => o.value);
    assert.equal(opciones.length, 11);
    controles[1].value = '2500';
    controles[1].listeners.input();
    assert.deepEqual(llamadas.at(-1), ['cambio', 0, 'base', '2500']);
    let quitar = null;
    recorrer(fila, (x) => { if (x.attrs['data-accion'] === 'quitar') quitar = x; });
    quitar.listeners.click();
    assert.deepEqual(llamadas.at(-1), ['quitar', 0]);
});

test('renderMensajes: errores y avisos por textContent; un texto malicioso no se interpreta como HTML', () => {
    const d = cargar();
    const cont = crear('div');
    d.renderMensajes(documento(), cont, { errores: [MAL], advertencias: ['aviso ' + MAL] });
    const t = textoDe(cont);
    assert.ok(t.includes(MAL));
    assert.equal(cont.hijos.length, 2);
    d.renderMensajes(documento(), cont, { errores: [], advertencias: [] });
    assert.equal(cont.hijos.length, 0);
});

const CUENTA = (extra = {}) => ({ id: 9, numero: 'CPP-2026-00009', proveedorNombre: MAL, numeroComprobanteProveedor: 'FE-' + MAL,
    claveComprobante: null, fechaEmisionComprobante: '2026-11-20', estado: 'PENDIENTE', moneda: 'CRC', monto: 113000,
    saldoPendiente: 113000, subtotal: 100000, impuesto: 13000,
    desglose: [{ codigoTarifa: '08', porcentaje: 13, base: 100000, iva: 13000 }, { codigoTarifa: null, porcentaje: null, base: 5, iva: 0 }],
    ...extra });

test('renderDetalle: número y fecha del comprobante, desglose y «sin tarifa», todo con textContent', () => {
    const d = cargar();
    const cont = crear('div');
    d.renderDetalle(documento(), cont, CUENTA());
    const t = textoDe(cont);
    assert.ok(t.includes('FE-' + MAL));
    assert.ok(t.includes('2026-11-20'));
    assert.ok(t.includes('13'));
    assert.ok(t.includes('Sin tarifa'));
    assert.ok(t.includes(MAL));
});

test('puedeAnular: solo con permiso y en una cuenta no anulada ni pagada', () => {
    const d = cargar();
    assert.equal(d.puedeAnular(CUENTA(), true), true);
    assert.equal(d.puedeAnular(CUENTA(), false), false);
    assert.equal(d.puedeAnular(CUENTA({ estado: 'ANULADA' }), true), false);
    assert.equal(d.puedeAnular(CUENTA({ estado: 'PAGADA' }), true), false);
});

test('validarMotivo: entre 3 y 180 caracteres tras recortar', () => {
    const d = cargar();
    assert.match(d.validarMotivo('ab'), /entre 3 y 180/);
    assert.match(d.validarMotivo('x'.repeat(181)), /entre 3 y 180/);
    assert.equal(d.validarMotivo('  abc  '), null);
    assert.equal(d.validarMotivo('x'.repeat(180)), null);
    assert.match(d.validarMotivo(null), /entre 3 y 180/);
});

test('el archivo no usa innerHTML, outerHTML ni insertAdjacentHTML', () => {
    const src = fs.readFileSync(ARCHIVO, 'utf8');
    assert.doesNotMatch(src, /innerHTML|outerHTML|insertAdjacentHTML|document\.write/);
});

const PAGO = (extra = {}) => ({ numero: 'PP-1', fecha: '2026-11-20', monto: 100, moneda: 'USD', tipoCambio: 500,
    metodoPago: 'EFECTIVO', cuentaBancaria: null, referencia: MAL, estado: 'CONFIRMADO', ...extra });

test('renderHistorial: pinta los pagos con textContent; la referencia con HTML se muestra como texto', () => {
    const d = cargar();
    const tbody = crear('tbody');
    d.renderHistorial(documento(), tbody, [PAGO(), PAGO({ numero: 'PP-2', estado: 'ANULADO', referencia: null, cuentaBancaria: MAL })]);
    assert.equal(tbody.hijos.length, 2);
    const t = textoDe(tbody);
    assert.ok(t.includes(MAL));
    assert.ok(t.includes('PP-2'));
    assert.ok(t.includes('ANULADO'));
    assert.ok(t.includes('TC 500'));
});

test('renderHistorial: sin pagos muestra el mensaje de vacío', () => {
    const d = cargar();
    const tbody = crear('tbody');
    d.renderHistorial(documento(), tbody, []);
    assert.ok(textoDe(tbody).includes('Sin pagos registrados'));
});

test('la plantilla no arma el historial de pagos con innerHTML', () => {
    const html = fs.readFileSync(path.join(raiz, 'src/main/resources/templates/modules/contabilidad/cuentas-pagar.html'), 'utf8');
    const f = html.slice(html.indexOf('async function abrirModalHistorial'), html.indexOf('// ── Toast helper'));
    assert.doesNotMatch(f, /innerHTML|insertAdjacentHTML/);
});
