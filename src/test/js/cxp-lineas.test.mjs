// T-052/T-054 (ADR-066, ADR-067): líneas de la cuenta por pagar con cuenta y tratamiento de IVA. Cálculo en vivo por
// tratamiento, validaciones (espejo de las del servidor), avisos y pintado solo con textContent.
//   node --test src/test/js/*.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const ARCHIVO = path.join(raiz, 'src/main/resources/static/modules/contabilidad/js/cxp-lineas.js');
const MAL = '<img src=x onerror=alert(1)>';

function cargar() {
    const ctx = { window: {}, console, Number, Math, Date, String, Array, Object };
    vm.createContext(ctx);
    vm.runInContext(fs.readFileSync(ARCHIVO, 'utf8'), ctx);
    return ctx.window.CxpLineas;
}

function crear(tag) {
    const e = {
        tag, hijos: [], attrs: {}, className: '', listeners: {}, _texto: '', value: '', disabled: false, style: {},
        appendChild(h) { this.hijos.push(h); return h; },
        setAttribute(k, v) { this.attrs[k] = String(v); },
        getAttribute(k) { return this.attrs[k] ?? null; },
        addEventListener(ev, fn) { this.listeners[ev] = fn; },
        replaceChildren() { this.hijos = []; this._texto = ''; },
        get children() { return this.hijos; },
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
    assert.equal(d.TARIFAS.find((t) => t.codigo === '10').porcentaje, 0);
    assert.equal(d.porcentajeDe('04'), 4);
    assert.equal(d.porcentajeDe('99'), null);
});

test('tratamientos: pleno, uso común, limitado y no acreditable, con etiqueta', () => {
    const d = cargar();
    assert.deepEqual([...d.TRATAMIENTOS.map((t) => t.codigo)], ['PLENO', 'USO_COMUN', 'LIMITADO', 'NO_ACREDITABLE']);
    assert.ok(d.TRATAMIENTOS.every((t) => t.etiqueta.length > 0));
});

test('lineaPorDefecto: tarifa 13 %, tratamiento PLENO y sin cuenta preseleccionada', () => {
    const d = cargar();
    const l = d.lineaPorDefecto();
    assert.equal(l.codigoTarifa, '08');
    assert.equal(l.tratamiento, 'PLENO');
    assert.equal(l.cuentaId, '');
    assert.equal(l.base, '');
    assert.equal(l.tarifaLimite, '');
});

test('calcularLinea PLENO: acreditable = IVA y no acreditable 0', () => {
    const d = cargar();
    const r = d.calcularLinea({ codigoTarifa: '08', base: '1000', iva: '130', tratamiento: 'PLENO' }, null);
    assert.equal(r.iva, 130);
    assert.equal(r.acreditable, 130);
    assert.equal(r.noAcreditable, 0);
    assert.equal(r.sinProrrata, false);
});

test('calcularLinea USO_COMUN: IVA × porcentaje del año (mitad hacia arriba); sin porcentaje se marca sinProrrata', () => {
    const d = cargar();
    const l = { codigoTarifa: '08', base: '1000', iva: '130', tratamiento: 'USO_COMUN' };
    const r = d.calcularLinea(l, 85.5);
    assert.equal(r.acreditable, 111.15);
    assert.equal(r.noAcreditable, 18.85);
    assert.equal(d.calcularLinea({ ...l, iva: '0.05' }, 50).acreditable, 0.03); // 0.025 → 0.03
    const sin = d.calcularLinea(l, null);
    assert.equal(sin.sinProrrata, true);
    assert.equal(sin.acreditable, 0);
    assert.equal(d.calcularLinea(l, 0).sinProrrata, false);
    assert.equal(d.calcularLinea(l, 0).acreditable, 0);
    assert.equal(d.calcularLinea(l, 100).acreditable, 130);
});

test('calcularLinea LIMITADO: compra al 13 % limitada al 1 % → acreditable 10,00 y no acreditable 120,00', () => {
    const d = cargar();
    const r = d.calcularLinea({ codigoTarifa: '08', base: '1000', iva: '130', tratamiento: 'LIMITADO', tarifaLimite: '1' }, null);
    assert.equal(r.acreditable, 10);
    assert.equal(r.noAcreditable, 120);
    // el límite nunca supera el IVA de la línea
    const tope = d.calcularLinea({ codigoTarifa: '02', base: '1000', iva: '10', tratamiento: 'LIMITADO', tarifaLimite: '5' }, null);
    assert.equal(tope.acreditable, 10);
    assert.equal(tope.noAcreditable, 0);
    assert.equal(d.calcularLinea({ codigoTarifa: '08', base: '1000', iva: '130', tratamiento: 'LIMITADO', tarifaLimite: '' }, null).acreditable, 0);
});

test('calcularLinea NO_ACREDITABLE: acreditable 0 y todo el IVA es no acreditable', () => {
    const d = cargar();
    const r = d.calcularLinea({ codigoTarifa: '08', base: '1000', iva: '130', tratamiento: 'NO_ACREDITABLE' }, 90);
    assert.equal(r.acreditable, 0);
    assert.equal(r.noAcreditable, 130);
});

test('cambiarLinea: base o tarifa recalculan el IVA; el tratamiento distinto de LIMITADO limpia la tarifa límite', () => {
    const d = cargar();
    let l = d.lineaPorDefecto();
    l = d.cambiarLinea(l, 'base', '1000');
    assert.equal(l.iva, '130');
    l = d.cambiarLinea(l, 'codigoTarifa', '04');
    assert.equal(l.iva, '40');
    l = d.cambiarLinea(l, 'iva', '41.5');
    assert.equal(l.iva, '41.5');
    l = d.cambiarLinea(l, 'tratamiento', 'LIMITADO');
    l = d.cambiarLinea(l, 'tarifaLimite', '1');
    assert.equal(l.tarifaLimite, '1');
    l = d.cambiarLinea(l, 'tratamiento', 'PLENO');
    assert.equal(l.tarifaLimite, '');
    assert.equal(d.cambiarLinea(d.lineaPorDefecto(), 'descripcion', 'x').descripcion, 'x');
});

test('totales: subtotal, impuesto, total, acreditable y no acreditable de todas las líneas', () => {
    const d = cargar();
    const t = d.totales([
        { codigoTarifa: '08', base: '1000', iva: '130', tratamiento: 'PLENO' },
        { codigoTarifa: '08', base: '1000', iva: '130', tratamiento: 'LIMITADO', tarifaLimite: '1' },
        { codigoTarifa: '08', base: '200', iva: '26', tratamiento: 'USO_COMUN' },
        { codigoTarifa: '08', base: '', iva: '' }], 50);
    assert.equal(t.subtotal, 2200);
    assert.equal(t.impuesto, 286);
    assert.equal(t.total, 2486);
    assert.equal(t.acreditable, 153);       // 130 + 10 + 13
    assert.equal(t.noAcreditable, 133);     // 0 + 120 + 13
    assert.deepEqual({ ...d.totales([], null) }, { subtotal: 0, impuesto: 0, total: 0, acreditable: 0, noAcreditable: 0 });
});

test('totales: sin error de coma flotante al sumar centavos', () => {
    const d = cargar();
    const t = d.totales([{ base: '0.10', iva: '0.01', tratamiento: 'PLENO' }, { base: '0.20', iva: '0.03', tratamiento: 'PLENO' }], null);
    assert.equal(t.subtotal, 0.3);
    assert.equal(t.impuesto, 0.04);
    assert.equal(t.total, 0.34);
});

test('usaProrrata y anioDe: detectan líneas de uso común y el año de la fecha del comprobante', () => {
    const d = cargar();
    assert.equal(d.usaProrrata([{ tratamiento: 'PLENO' }, { tratamiento: 'USO_COMUN' }]), true);
    assert.equal(d.usaProrrata([{ tratamiento: 'PLENO' }]), false);
    assert.equal(d.anioDe('2026-11-20'), 2026);
    assert.equal(d.anioDe(''), null);
    assert.equal(d.anioDe('abc'), null);
});

const LINEA = (extra = {}) => ({ cuentaId: '7', descripcion: 'Mercadería', codigoTarifa: '08', base: '100000', iva: '13000',
    tratamiento: 'PLENO', tarifaLimite: '', ...extra });
const OK = () => ({ numero: 'FE-1', clave: '', fecha: '2026-11-20', hoy: '2026-11-22', lineas: [LINEA()], prorrata: null, totalComprobante: '' });

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
});

test('validar: al menos una línea; cada línea con cuenta, base mayor a 0 e IVA no negativo', () => {
    const d = cargar();
    assert.match(d.validar({ ...OK(), lineas: [] }).errores[0], /al menos una línea/);
    assert.match(d.validar({ ...OK(), lineas: [LINEA({ cuentaId: '' })] }).errores[0], /línea 1 requiere una cuenta/);
    assert.match(d.validar({ ...OK(), lineas: [LINEA({ base: '' })] }).errores[0], /línea 1 requiere una base mayor a 0/);
    assert.match(d.validar({ ...OK(), lineas: [LINEA({ base: '0' })] }).errores[0], /base mayor a 0/);
    assert.match(d.validar({ ...OK(), lineas: [LINEA({ iva: '-1' })] }).errores[0], /IVA no negativo/);
    assert.match(d.validar({ ...OK(), lineas: [LINEA({ codigoTarifa: '99' })] }).errores[0], /tarifa de IVA de la línea 1/);
});

test('validar: LIMITADO exige una tarifa límite mayor a 0 y menor que la tarifa de la línea', () => {
    const d = cargar();
    const lim = (v) => d.validar({ ...OK(), lineas: [LINEA({ tratamiento: 'LIMITADO', tarifaLimite: v })] });
    assert.match(lim('').errores[0], /tarifa límite/);
    assert.match(lim('0').errores[0], /tarifa límite/);
    assert.match(lim('13').errores[0], /tarifa límite/);
    assert.match(lim('14').errores[0], /tarifa límite/);
    assert.equal(lim('1').errores.length, 0);
});

test('validar: USO_COMUN sin porcentaje de prorrata para el año del comprobante avisa con el año', () => {
    const d = cargar();
    const datos = { ...OK(), lineas: [LINEA({ tratamiento: 'USO_COMUN' })] };
    const sin = d.validar({ ...datos, prorrata: null });
    assert.equal(sin.errores.length, 0);
    assert.equal(sin.advertencias.length, 1);
    assert.match(sin.advertencias[0], /No hay porcentaje de prorrata configurado para 2026/);
    assert.equal(d.validar({ ...datos, prorrata: 80 }).advertencias.length, 0);
    assert.equal(d.validar({ ...datos, prorrata: 0 }).advertencias.length, 0);
    assert.equal(d.validar({ ...OK(), prorrata: null }).advertencias.length, 0); // sin líneas de uso común no avisa
});

test('validar: IVA distinto de base × tarifa por más de 0,01 es un aviso; dentro de la tolerancia, no', () => {
    const d = cargar();
    const aviso = d.validar({ ...OK(), lineas: [LINEA({ iva: '12999' })] });
    assert.equal(aviso.errores.length, 0);
    assert.equal(aviso.advertencias.length, 1);
    assert.match(aviso.advertencias[0], /línea 1/);
    assert.equal(d.validar({ ...OK(), lineas: [LINEA({ iva: '13000.01' })] }).advertencias.length, 0);
});

test('validar: avisa si el total del comprobante no cuadra con la suma de las líneas', () => {
    const d = cargar();
    assert.equal(d.validar({ ...OK(), totalComprobante: '113000' }).advertencias.length, 0);
    const r = d.validar({ ...OK(), totalComprobante: '120000' });
    assert.equal(r.advertencias.length, 1);
    assert.match(r.advertencias[0], /no cuadra/);
});

test('armarPeticion: líneas con cuenta, tarifa, base, IVA y tratamiento; tarifa límite solo en LIMITADO; sin totales del cliente', () => {
    const d = cargar();
    const p = d.armarPeticion({ numero: ' FE-1 ', clave: '', fecha: '2026-11-20',
        lineas: [LINEA({ cuentaId: '7', descripcion: ' Mercadería ' }),
            LINEA({ cuentaId: '8', base: '1000', iva: '130', tratamiento: 'LIMITADO', tarifaLimite: '1' }),
            LINEA({ cuentaId: '9', base: '50', iva: '6.5', tratamiento: 'PLENO', tarifaLimite: '3' })] });
    assert.equal(p.numeroComprobanteProveedor, 'FE-1');
    assert.equal(p.claveComprobante, null);
    assert.equal(p.fechaComprobante, '2026-11-20');
    assert.equal(p.fechaEmision, '2026-11-20');
    assert.equal(p.subtotal, undefined);
    assert.equal(p.monto, undefined);
    assert.equal(p.lineas.length, 3);
    assert.deepEqual({ ...p.lineas[0] }, { descripcion: 'Mercadería', cuentaContableId: 7, codigoTarifa: '08', base: 100000, iva: 13000,
        tratamiento: 'PLENO', tarifaLimite: null });
    assert.equal(p.lineas[1].tarifaLimite, 1);
    assert.equal(p.lineas[1].tratamiento, 'LIMITADO');
    assert.equal(p.lineas[2].tarifaLimite, null);
});

const CUENTAS = [
    { id: 7, codigo: '5.3.01.001', nombre: 'Compras de mercadería' },
    { id: 8, codigo: '5.1.03.001', nombre: 'Gasto en Servicios Públicos' },
    { id: 9, codigo: '1.1.04.001', nombre: 'Inventario de mercadería' }
];

test('filtrarCuentas: busca por código o nombre sin distinguir mayúsculas ni tildes', () => {
    const d = cargar();
    assert.equal(d.filtrarCuentas(CUENTAS, '').length, 3);
    assert.deepEqual([...d.filtrarCuentas(CUENTAS, 'MERCADERIA').map((c) => c.id)], [7, 9]);
    assert.deepEqual([...d.filtrarCuentas(CUENTAS, '5.1.03').map((c) => c.id)], [8]);
    assert.deepEqual([...d.filtrarCuentas(CUENTAS, ' servicios ').map((c) => c.id)], [8]);
    assert.deepEqual([...d.filtrarCuentas(CUENTAS, 'zzz')], []);
    assert.deepEqual([...d.filtrarCuentas(null, 'a')], []);
});

function controlesDe(fila) {
    const c = {};
    recorrer(fila, (x) => { if (x.attrs['data-campo']) c[x.attrs['data-campo']] = x; });
    return c;
}

test('renderLinea: editor con cuenta (buscador + selector de hojas), descripción, tarifa, base, tratamiento y tarifa límite', () => {
    const d = cargar();
    const llamadas = [];
    const fila = d.renderLinea(documento(), LINEA({ cuentaId: '7' }), 0, CUENTAS,
        { cambio: (i, campo, valor) => llamadas.push(['cambio', i, campo, valor]), quitar: (i) => llamadas.push(['quitar', i]) });
    assert.equal(fila.tag, 'tr');
    const c = controlesDe(fila);
    assert.deepEqual(Object.keys(c).sort(), ['base', 'buscarCuenta', 'codigoTarifa', 'cuentaId', 'descripcion', 'iva', 'tarifaLimite', 'tratamiento']);
    assert.equal(c.codigoTarifa.value, '08');
    assert.equal(c.codigoTarifa.hijos.length, 11);
    assert.equal(c.tratamiento.value, 'PLENO');
    assert.equal(c.tratamiento.hijos.length, 4);
    assert.equal(c.cuentaId.value, '7');
    assert.equal(c.cuentaId.hijos.length, 4);           // «— Seleccionar —» + 3 hojas
    assert.ok(textoDe(c.cuentaId).includes('5.3.01.001 - Compras de mercadería'));

    c.base.value = '2500';
    c.base.listeners.input();
    assert.deepEqual(llamadas.at(-1), ['cambio', 0, 'base', '2500']);
    c.tratamiento.value = 'LIMITADO';
    c.tratamiento.listeners.change();
    assert.deepEqual(llamadas.at(-1), ['cambio', 0, 'tratamiento', 'LIMITADO']);
    c.cuentaId.value = '8';
    c.cuentaId.listeners.change();
    assert.deepEqual(llamadas.at(-1), ['cambio', 0, 'cuentaId', '8']);
    let quitar = null;
    recorrer(fila, (x) => { if (x.attrs['data-accion'] === 'quitar') quitar = x; });
    quitar.listeners.click();
    assert.deepEqual(llamadas.at(-1), ['quitar', 0]);
});

test('renderLinea: escribir en el buscador filtra las hojas del selector sin perder la cuenta elegida', () => {
    const d = cargar();
    const fila = d.renderLinea(documento(), LINEA({ cuentaId: '8' }), 0, CUENTAS, { cambio() {}, quitar() {} });
    const c = controlesDe(fila);
    c.buscarCuenta.value = 'inventario';
    c.buscarCuenta.listeners.input();
    const opciones = c.cuentaId.hijos.map((o) => o.value);
    assert.deepEqual(opciones, ['', '9', '8']);          // vacío, coincidencia y la elegida se conserva
    c.buscarCuenta.value = '';
    c.buscarCuenta.listeners.input();
    assert.equal(c.cuentaId.hijos.length, 4);
});

test('actualizarLinea: muestra IVA, acreditable y no acreditable en vivo y la tarifa límite solo en LIMITADO', () => {
    const d = cargar();
    const l = LINEA({ base: '1000', iva: '130', tratamiento: 'LIMITADO', tarifaLimite: '1' });
    const fila = d.renderLinea(documento(), l, 0, CUENTAS, { cambio() {}, quitar() {} });
    d.actualizarLinea(fila, l, 80);
    let acre = null; let noAcre = null; let envoltura = null;
    recorrer(fila, (x) => {
        if (x.attrs['data-calculado'] === 'acreditable') acre = x;
        if (x.attrs['data-calculado'] === 'noAcreditable') noAcre = x;
        if (x.attrs['data-envoltura'] === 'tarifaLimite') envoltura = x;
    });
    assert.equal(acre.textContent, '10.00');
    assert.equal(noAcre.textContent, '120.00');
    assert.notEqual(envoltura.style.display, 'none');
    const pleno = { ...l, tratamiento: 'PLENO' };
    d.actualizarLinea(fila, pleno, 80);
    assert.equal(acre.textContent, '130.00');
    assert.equal(noAcre.textContent, '0.00');
    assert.equal(envoltura.style.display, 'none');
    const uso = { ...l, tratamiento: 'USO_COMUN' };
    d.actualizarLinea(fila, uso, null);
    assert.equal(acre.textContent, '—');
    d.actualizarLinea(fila, uso, 80);
    assert.equal(acre.textContent, '104.00');
});

test('renderLineas: una fila por línea y se vacía antes de repintar', () => {
    const d = cargar();
    const tbody = crear('tbody');
    d.renderLineas(documento(), tbody, [LINEA(), LINEA()], CUENTAS, { cambio() {}, quitar() {} }, null);
    assert.equal(tbody.hijos.length, 2);
    d.renderLineas(documento(), tbody, [LINEA()], CUENTAS, { cambio() {}, quitar() {} }, null);
    assert.equal(tbody.hijos.length, 1);
});

test('renderLinea: una descripción o una cuenta con HTML se pinta como texto (la fake DOM falla si se usa innerHTML)', () => {
    const d = cargar();
    const cuentas = [{ id: 1, codigo: '5.1', nombre: MAL }];
    const fila = d.renderLinea(documento(), LINEA({ descripcion: MAL, cuentaId: '1' }), 0, cuentas, { cambio() {}, quitar() {} });
    assert.ok(textoDe(fila).includes(MAL));
    assert.equal(controlesDe(fila).descripcion.value, MAL);
});

test('renderMensajes: errores y avisos por textContent; un texto malicioso no se interpreta como HTML', () => {
    const d = cargar();
    const cont = crear('div');
    d.renderMensajes(documento(), cont, { errores: [MAL], advertencias: ['aviso ' + MAL] });
    assert.ok(textoDe(cont).includes(MAL));
    assert.equal(cont.hijos.length, 2);
    d.renderMensajes(documento(), cont, { errores: [], advertencias: [] });
    assert.equal(cont.hijos.length, 0);
});

const CUENTA = (extra = {}) => ({ id: 9, numero: 'CPP-2026-00009', proveedorNombre: MAL, numeroComprobanteProveedor: 'FE-' + MAL,
    claveComprobante: null, fechaEmisionComprobante: '2026-11-20', estado: 'PENDIENTE', moneda: 'CRC', monto: 113000,
    saldoPendiente: 113000, subtotal: 100000, impuesto: 13000,
    estadoAceptacion: 'PENDIENTE', fechaAceptacion: null, numeroMensajeReceptor: null, diasDesdeComprobante: 2,
    lineas: [
        { numeroLinea: 1, descripcion: MAL, cuentaCodigo: '5.3.01.001', cuentaNombre: 'Compras de mercadería', codigoTarifa: '08',
            porcentaje: 13, base: 1000, iva: 130, tratamiento: 'LIMITADO', tarifaLimite: 1, porcentajeProrrata: null,
            ivaAcreditable: 10, ivaNoAcreditable: 120 },
        { numeroLinea: 2, descripcion: 'Sin tarifa', cuentaCodigo: '5.1.01', cuentaNombre: 'Compras', codigoTarifa: null,
            porcentaje: null, base: 5, iva: 0, tratamiento: 'PLENO', tarifaLimite: null, porcentajeProrrata: null,
            ivaAcreditable: 0, ivaNoAcreditable: 0 },
        { numeroLinea: 3, descripcion: 'Uso común', cuentaCodigo: '5.1.01', cuentaNombre: 'Compras', codigoTarifa: '08',
            porcentaje: 13, base: 100, iva: 13, tratamiento: 'USO_COMUN', tarifaLimite: null, porcentajeProrrata: 85.5,
            ivaAcreditable: 11.12, ivaNoAcreditable: 1.88 }],
    ...extra });

test('renderDetalle: comprobante, líneas con cuenta y tratamiento, «sin tarifa» y aceptación, todo con textContent', () => {
    const d = cargar();
    const cont = crear('div');
    d.renderDetalle(documento(), cont, CUENTA());
    const t = textoDe(cont);
    assert.ok(t.includes('FE-' + MAL));
    assert.ok(t.includes('2026-11-20'));
    assert.ok(t.includes(MAL));
    assert.ok(t.includes('5.3.01.001'));
    assert.ok(t.includes('Compras de mercadería'));
    assert.ok(t.includes('Sin tarifa'));
    assert.ok(t.includes('120.00'));
    assert.ok(t.includes('Limitado'));
    assert.ok(t.includes('85.5'));
    assert.ok(t.includes('Aceptación'));
    assert.ok(t.includes('PENDIENTE'));
    assert.ok(t.includes('2 días'));
});

test('renderDetalle: con aceptación registrada muestra el estado, la fecha y el número del mensaje receptor', () => {
    const d = cargar();
    const cont = crear('div');
    d.renderDetalle(documento(), cont, CUENTA({ estadoAceptacion: 'ACEPTADO', fechaAceptacion: '2026-11-21', numeroMensajeReceptor: 'MR-' + MAL,
        diasDesdeComprobante: null }));
    const t = textoDe(cont);
    assert.ok(t.includes('ACEPTADO'));
    assert.ok(t.includes('2026-11-21'));
    assert.ok(t.includes('MR-' + MAL));
    assert.ok(!t.includes('días'));
});

test('validarAceptacion: estado ACEPTADO o RECHAZADO, fecha no futura ni anterior al comprobante, número obligatorio de hasta 50', () => {
    const d = cargar();
    const base = { estado: 'ACEPTADO', fecha: '2026-11-21', numero: 'MR-1', hoy: '2026-11-22', fechaComprobante: '2026-11-20' };
    assert.equal(d.validarAceptacion(base), null);
    assert.equal(d.validarAceptacion({ ...base, estado: 'RECHAZADO', fecha: '2026-11-20' }), null);
    assert.match(d.validarAceptacion({ ...base, estado: 'PENDIENTE' }), /Aceptado o Rechazado/);
    assert.match(d.validarAceptacion({ ...base, fecha: '' }), /fecha de aceptación es obligatoria/);
    assert.match(d.validarAceptacion({ ...base, fecha: '2026-11-23' }), /no puede ser futura/);
    assert.match(d.validarAceptacion({ ...base, fecha: '2026-11-19' }), /anterior a la fecha del comprobante/);
    assert.match(d.validarAceptacion({ ...base, numero: '  ' }), /número del mensaje receptor es obligatorio/);
    assert.match(d.validarAceptacion({ ...base, numero: 'N'.repeat(51) }), /50 caracteres/);
});

test('puedeRegistrarAceptacion: con permiso, en una CxP no anulada', () => {
    const d = cargar();
    assert.equal(d.puedeRegistrarAceptacion(CUENTA(), true), true);
    assert.equal(d.puedeRegistrarAceptacion(CUENTA(), false), false);
    assert.equal(d.puedeRegistrarAceptacion(CUENTA({ estado: 'ANULADA' }), true), false);
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

// ── T-053 revisión 1 (criterio 15): fecha de vencimiento propuesta ───────────────────────────────────────────────────

test('calcularVencimiento: fecha del comprobante + días de crédito; sin días (0, nulo, vacío) es de contado', () => {
    const d = cargar();
    assert.equal(d.calcularVencimiento('2026-09-01', 30), '2026-10-01');
    assert.equal(d.calcularVencimiento('2026-09-01', '30'), '2026-10-01');
    assert.equal(d.calcularVencimiento('2026-12-15', 30), '2027-01-14');   // cruza de año
    assert.equal(d.calcularVencimiento('2028-02-01', 29), '2028-03-01');   // bisiesto
    assert.equal(d.calcularVencimiento('2026-09-01', 0), '2026-09-01');
    assert.equal(d.calcularVencimiento('2026-09-01', null), '2026-09-01');
    assert.equal(d.calcularVencimiento('2026-09-01', ''), '2026-09-01');
    assert.equal(d.calcularVencimiento('2026-09-01', -5), '2026-09-01');
});

test('calcularVencimiento: sin fecha de comprobante válida no propone nada', () => {
    const d = cargar();
    assert.equal(d.calcularVencimiento('', 30), '');
    assert.equal(d.calcularVencimiento(null, 30), '');
    assert.equal(d.calcularVencimiento('no-es-fecha', 30), '');
});

test('validar: el vencimiento anterior al comprobante es error con el mensaje fijo del servidor; igual o posterior pasa', () => {
    const d = cargar();
    const base = { numero: 'FE-1', fecha: '2026-09-10', hoy: '2026-11-01', lineas: [{ cuentaId: '1', base: '100', iva: '13', codigoTarifa: '08', tratamiento: 'PLENO' }] };
    const msg = 'La fecha de vencimiento no puede ser anterior a la fecha del comprobante';
    assert.ok(d.validar({ ...base, vencimiento: '2026-09-09' }).errores.includes(msg));
    assert.ok(!d.validar({ ...base, vencimiento: '2026-09-10' }).errores.includes(msg));
    assert.ok(!d.validar({ ...base, vencimiento: '' }).errores.includes(msg));
});

test('armarPeticion incluye fechaVencimiento solo si el usuario la dejó con valor', () => {
    const d = cargar();
    const datos = { numero: 'A', fecha: '2026-09-10', lineas: [] };
    assert.equal(d.armarPeticion({ ...datos, vencimiento: '2026-10-10' }).fechaVencimiento, '2026-10-10');
    assert.equal(d.armarPeticion({ ...datos, vencimiento: '' }).fechaVencimiento, null);
});

test('las plantillas de CxP manual y de factura desde la OC proponen el vencimiento con CxpLineas.calcularVencimiento', () => {
    const t = (r) => fs.readFileSync(path.join(raiz, 'src/main/resources/templates/modules', r), 'utf8');
    assert.match(t('contabilidad/cuentas-pagar.html'), /CxpLineas\.calcularVencimiento/);
    assert.match(t('proveedores/ordenes-compra.html'), /CxpLineas\.calcularVencimiento/);
});

// ── T-058 (ADR-074 §1, §5): política de IVA por fecha; el JS solo aplica lo que dice el endpoint ──────────────────────

const REGLA_FIJA = {
    regimen: 'SIMPLIFICADO', tratamientosAdmitidos: ['NO_ACREDITABLE'], tratamientoFijo: 'NO_ACREDITABLE',
    ivaTrasladadoCero: false, mensaje: 'Régimen simplificado: el IVA de la compra no es acreditable'
};
const REGLA_LIBRE = {
    regimen: 'GENERAL', tratamientosAdmitidos: ['PLENO', 'USO_COMUN', 'LIMITADO', 'NO_ACREDITABLE'], tratamientoFijo: null,
    ivaTrasladadoCero: false, mensaje: ''
};

test('T-058 renderLinea con una regla de tratamiento fijo: el select queda preseleccionado y deshabilitado', () => {
    const d = cargar();
    const fila = d.renderLinea(documento(), LINEA({ cuentaId: '7', tratamiento: 'PLENO' }), 0, CUENTAS,
        { cambio() {}, quitar() {} }, REGLA_FIJA);
    const c = controlesDe(fila);
    assert.equal(c.tratamiento.value, 'NO_ACREDITABLE');
    assert.equal(c.tratamiento.disabled, true);
    const libre = controlesDe(d.renderLinea(documento(), LINEA({ cuentaId: '7' }), 0, CUENTAS, { cambio() {}, quitar() {} }, REGLA_LIBRE));
    assert.equal(libre.tratamiento.disabled, false);
    assert.equal(libre.tratamiento.value, 'PLENO');
});

test('T-058 aplicarPolitica fija el tratamiento solo cuando la regla lo fija; sin regla no cambia nada', () => {
    const d = cargar();
    const lineas = [LINEA({ tratamiento: 'PLENO' }), LINEA({ tratamiento: 'USO_COMUN' })];
    assert.deepEqual([...d.aplicarPolitica(lineas, REGLA_FIJA).map((l) => l.tratamiento)], ['NO_ACREDITABLE', 'NO_ACREDITABLE']);
    assert.deepEqual([...d.aplicarPolitica(lineas, REGLA_LIBRE).map((l) => l.tratamiento)], ['PLENO', 'USO_COMUN']);
    assert.deepEqual([...d.aplicarPolitica(lineas, null).map((l) => l.tratamiento)], ['PLENO', 'USO_COMUN']);
    assert.equal(lineas[0].tratamiento, 'PLENO');   // no muta la entrada
    assert.equal(d.lineaPorDefecto(REGLA_FIJA).tratamiento, 'NO_ACREDITABLE');
    assert.equal(d.lineaPorDefecto(null).tratamiento, 'PLENO');
});

test('T-058 renderPolitica pinta el mensaje con textContent; uno hostil queda como texto y sin mensaje se limpia', () => {
    const d = cargar();
    const cont = crear('div');
    d.renderPolitica(documento(), cont, { ...REGLA_FIJA, mensaje: MAL });
    assert.equal(cont.hijos.length, 1);
    assert.equal(cont.hijos[0].textContent, MAL);
    d.renderPolitica(documento(), cont, REGLA_LIBRE);
    assert.equal(cont.hijos.length, 0);
    d.renderPolitica(documento(), cont, { error: MAL });
    assert.equal(cont.hijos[0].textContent, MAL);
    assert.ok(cont.hijos[0].className.includes('alert-danger'));
});

test('T-058 consultarPolitica: arma la URL con fecha y proveedor, devuelve la regla o el mensaje del 409', async () => {
    const d = cargar();
    const urls = [];
    const ok = async (u) => { urls.push(u); return { ok: true, status: 200, json: async () => ({ success: true, data: REGLA_FIJA }) }; };
    assert.deepEqual(await d.consultarPolitica(ok, '2027-02-01', 5), REGLA_FIJA);
    assert.equal(urls[0], '/api/cuentas-pagar/politica-iva?fecha=2027-02-01&proveedorId=5');
    await d.consultarPolitica(ok, '2027-02-01', null);
    assert.equal(urls[1], '/api/cuentas-pagar/politica-iva?fecha=2027-02-01');
    const conflicto = async () => ({ ok: false, status: 409, json: async () => ({ success: false, message: MAL }) });
    assert.equal((await d.consultarPolitica(conflicto, '1999-01-01', null)).error, MAL);
    const caido = async () => { throw new Error('red'); };
    assert.ok((await d.consultarPolitica(caido, '2027-02-01', 5)).error.length > 0);
    assert.equal(await d.consultarPolitica(ok, '', 5), null);   // sin fecha no se consulta
});

test('T-058 validar: con proveedor SIMPLIFICADO (IVA trasladado 0) no avisa que el IVA difiere de base × tarifa', () => {
    const d = cargar();
    const datos = { numero: 'F-1', fecha: '2027-02-01', hoy: '2027-03-01',
        lineas: [LINEA({ cuentaId: '7', base: '1000', iva: '0', codigoTarifa: '08', tratamiento: 'NO_ACREDITABLE' })] };
    assert.equal(d.validar(datos).advertencias.length, 1);                       // hoy: avisa (IVA 0 ≠ 130)
    const r = d.validar({ ...datos, regla: { ...REGLA_FIJA, ivaTrasladadoCero: true } });
    assert.deepEqual([...r.errores], []);
    assert.deepEqual([...r.advertencias], []);
    assert.equal(d.validar({ ...datos, regla: REGLA_FIJA }).advertencias.length, 1);   // empresa SIMPLIFICADO sola no lo evita
});

test('T-058 cambiarLinea con regla de IVA trasladado 0: base y tarifa dejan el IVA en 0, sin tocar la tarifa del bien', () => {
    const d = cargar();
    const regla = { ...REGLA_FIJA, ivaTrasladadoCero: true };
    const n = d.cambiarLinea(LINEA({ codigoTarifa: '08' }), 'base', '1000', regla);
    assert.equal(n.iva, '0');
    assert.equal(n.codigoTarifa, '08');
    assert.equal(d.cambiarLinea(LINEA({ codigoTarifa: '08' }), 'base', '1000', null).iva, '130');
});

test('T-058 puedeRegistrarAceptacion: una CxP NO_APLICA (régimen simplificado) no se acepta ni se rechaza', () => {
    const d = cargar();
    assert.equal(d.puedeRegistrarAceptacion({ estado: 'PENDIENTE', estadoAceptacion: 'NO_APLICA' }, true), false);
    assert.equal(d.puedeRegistrarAceptacion({ estado: 'PENDIENTE', estadoAceptacion: 'PENDIENTE' }, true), true);
});
