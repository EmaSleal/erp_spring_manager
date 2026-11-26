// T-058 (ADR-077 §1-2, §7-9): formulario rápido «Compra de contado». Validaciones, líneas con cuenta 5.3.01.001 por defecto,
// tratamiento que fija la política, caja y EFECTIVO por defecto, armado del envío multipart, confirmación y errores, búsqueda y
// alta de proveedor, enlace de descarga del adjunto, todo pintado con textContent y sin innerHTML.
//   node --test src/test/js/*.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const ESTATICO = path.join(raiz, 'src/main/resources/static/modules');
const LINEAS = path.join(ESTATICO, 'contabilidad/js/cxp-lineas.js');
const COMPRA = path.join(ESTATICO, 'proveedores/js/compra-contado.js');
const PAGINA = path.join(ESTATICO, 'proveedores/js/compra-contado-pagina.js');
const PLANTILLA = path.join(raiz, 'src/main/resources/templates/modules/proveedores/compra-contado.html');
const MAL = '<img src=x onerror=alert(1)>';

function cargar() {
    const ctx = { window: {}, console, Number, Math, Date, String, Array, Object, Promise, JSON, encodeURIComponent, parseInt, parseFloat, isNaN };
    vm.createContext(ctx);
    vm.runInContext(fs.readFileSync(LINEAS, 'utf8'), ctx);
    vm.runInContext(fs.readFileSync(COMPRA, 'utf8'), ctx);
    return { c: ctx.window.CompraContado, l: ctx.window.CxpLineas };
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

const CUENTAS = [
    { id: 11, codigo: '5.2.01.001', nombre: 'Gastos varios' },
    { id: 42, codigo: '5.3.01.001', nombre: 'Compras de mercadería' }
];
const REGLA_RTS = { regimen: 'SIMPLIFICADO', tratamientosAdmitidos: ['NO_ACREDITABLE'], tratamientoFijo: 'NO_ACREDITABLE', ivaTrasladadoCero: true, mensaje: 'Proveedor simplificado' };
const REGLA_GENERAL = { regimen: 'GENERAL', tratamientosAdmitidos: ['PLENO', 'NO_ACREDITABLE'], tratamientoFijo: null, ivaTrasladadoCero: false, mensaje: '' };

function estadoValido(c, regla = REGLA_RTS) {
    let e = c.estadoInicial('2026-06-20', CUENTAS, regla);
    e = { ...e, proveedor: { id: 7, nombre: 'Monrachem', cedula: '3101123456', regimenHacienda: 'SIMPLIFICADO' }, numero: 'F-100' };
    e = c.cambiarLinea(e, 0, 'base', '1000', regla);
    return e;
}

const respuesta = (ok, status, cuerpo) => Promise.resolve({ ok, status, json: () => Promise.resolve(cuerpo) });

test('constantes: cuenta de compra 5.3.01.001, caja general y EFECTIVO por defecto; los cinco métodos', () => {
    const { c } = cargar();
    assert.equal(c.CODIGO_CUENTA_COMPRA, '5.3.01.001');
    assert.equal(c.ETIQUETA_CAJA, 'Caja general (1.1.01.001)');
    assert.deepEqual(Array.from(c.METODOS.map((m) => m.codigo)), ['EFECTIVO', 'TRANSFERENCIA', 'CHEQUE', 'SINPE', 'DEPOSITO']);
    assert.equal(c.requiereCuentaBancaria('EFECTIVO'), false);
    for (const m of ['TRANSFERENCIA', 'CHEQUE', 'SINPE', 'DEPOSITO']) assert.equal(c.requiereCuentaBancaria(m), true);
});

test('estado inicial: fecha de hoy, EFECTIVO, una línea con la cuenta 5.3.01.001 resuelta por código (no por id fijo)', () => {
    const { c } = cargar();
    const e = c.estadoInicial('2026-06-20', CUENTAS, null);
    assert.equal(e.fecha, '2026-06-20');
    assert.equal(e.metodoPago, 'EFECTIVO');
    assert.equal(e.cuentaBancariaId, '');
    assert.equal(e.lineas.length, 1);
    assert.equal(String(e.lineas[0].cuentaId), '42');
    assert.equal(e.lineas[0].codigoTarifa, '08');
    assert.equal(c.cuentaPorDefecto([{ id: 1, codigo: '9.9', nombre: 'x' }]), '');
    assert.equal(c.cuentaPorDefecto(CUENTAS), 42);
});

test('líneas: agregar conserva la cuenta por defecto y el tratamiento fijo; quitar nunca deja cero líneas', () => {
    const { c } = cargar();
    let e = c.estadoInicial('2026-06-20', CUENTAS, REGLA_RTS);
    e = c.agregarLinea(e, REGLA_RTS, CUENTAS);
    assert.equal(e.lineas.length, 2);
    assert.equal(e.lineas[1].tratamiento, 'NO_ACREDITABLE');
    assert.equal(String(e.lineas[1].cuentaId), '42');
    e = c.quitarLinea(e, 0);
    assert.equal(e.lineas.length, 1);
    e = c.quitarLinea(e, 0);
    assert.equal(e.lineas.length, 1, 'una compra tiene al menos una línea');
});

test('cambiarLinea con la regla: el tratamiento queda fijo y el IVA trasladado en 0 aunque la tarifa del bien sea 13 %', () => {
    const { c } = cargar();
    let e = c.estadoInicial('2026-06-20', CUENTAS, REGLA_RTS);
    e = c.cambiarLinea(e, 0, 'base', '1000', REGLA_RTS);
    e = c.cambiarLinea(e, 0, 'tratamiento', 'PLENO', REGLA_RTS);
    assert.equal(e.lineas[0].codigoTarifa, '08');
    assert.equal(e.lineas[0].iva, '0');
    assert.equal(e.lineas[0].tratamiento, 'NO_ACREDITABLE');
    let g = c.estadoInicial('2026-10-20', CUENTAS, REGLA_GENERAL);
    g = c.cambiarLinea(g, 0, 'base', '1000', REGLA_GENERAL);
    assert.equal(g.lineas[0].iva, '130');
    assert.equal(g.lineas[0].tratamiento, 'PLENO');
});

test('aplicarRegla: al cambiar fecha o proveedor las líneas toman el tratamiento fijo de la nueva regla', () => {
    const { c } = cargar();
    let e = estadoValido(c, REGLA_GENERAL);
    e = c.aplicarRegla(e, REGLA_RTS);
    assert.equal(e.lineas[0].tratamiento, 'NO_ACREDITABLE');
    assert.equal(e.lineas[0].iva, '0');
});

test('validar: falta proveedor, número, fecha y líneas', () => {
    const { c } = cargar();
    const e = { ...c.estadoInicial('', CUENTAS, null), lineas: [] };
    const { errores } = c.validar(e, { regla: null, hoy: '2026-06-20' });
    assert.ok(errores.includes('Seleccione o cree el proveedor'));
    assert.ok(errores.includes('El número del comprobante del proveedor es obligatorio'));
    assert.ok(errores.includes('La fecha de emisión del comprobante es obligatoria'));
    assert.ok(errores.includes('La cuenta por pagar debe tener al menos una línea'));
});

test('validar: monto ≤ 0 y cuenta vacía por línea', () => {
    const { c } = cargar();
    let e = estadoValido(c);
    e = c.cambiarLinea(e, 0, 'base', '0', REGLA_RTS);
    assert.ok(c.validar(e, { regla: REGLA_RTS, hoy: '2026-06-20' }).errores.some((m) => m.includes('base mayor a 0')));
    e = c.cambiarLinea(estadoValido(c), 0, 'cuentaId', '', REGLA_RTS);
    assert.ok(c.validar(e, { regla: REGLA_RTS, hoy: '2026-06-20' }).errores.some((m) => m.includes('requiere una cuenta contable')));
});

test('validar: IVA trasladado > 0 cuando la regla exige 0 es un error; con regla general no', () => {
    const { c } = cargar();
    let e = estadoValido(c);
    e = { ...e, lineas: [{ ...e.lineas[0], iva: '130' }] };
    const r = c.validar(e, { regla: REGLA_RTS, hoy: '2026-06-20' });
    assert.ok(r.errores.some((m) => m.includes('no traslada IVA')), r.errores.join('|'));
    let g = estadoValido(c, REGLA_GENERAL);
    g = { ...g, lineas: [{ ...g.lineas[0], iva: '130' }] };
    assert.equal(c.validar(g, { regla: REGLA_GENERAL, hoy: '2026-06-20' }).errores.length, 0);
});

test('validar: fecha futura; método distinto de EFECTIVO sin cuenta bancaria; con cuenta bancaria o EFECTIVO pasa', () => {
    const { c } = cargar();
    const e = estadoValido(c);
    assert.ok(c.validar({ ...e, fecha: '2026-07-01' }, { regla: REGLA_RTS, hoy: '2026-06-20' }).errores.some((m) => m.includes('futura')));
    const t = { ...e, metodoPago: 'TRANSFERENCIA', cuentaBancariaId: '' };
    assert.ok(c.validar(t, { regla: REGLA_RTS, hoy: '2026-06-20' }).errores.includes('Seleccione la cuenta bancaria de origen del pago'));
    assert.equal(c.validar({ ...t, cuentaBancariaId: '3' }, { regla: REGLA_RTS, hoy: '2026-06-20' }).errores.length, 0);
    assert.equal(c.validar(e, { regla: REGLA_RTS, hoy: '2026-06-20' }).errores.length, 0);
});

test('adjunto: extensiones XML, PDF, JPG, JPEG y PNG; tamaño máximo configurable (5 MB por defecto)', () => {
    const { c } = cargar();
    for (const n of ['a.xml', 'a.PDF', 'a.jpg', 'a.jpeg', 'a.png']) assert.equal(c.validarAdjunto({ name: n, size: 100 }), null, n);
    assert.equal(c.validarAdjunto({ name: 'a.exe', size: 100 }), 'El adjunto debe ser un archivo XML, PDF, JPG o PNG');
    assert.equal(c.validarAdjunto({ name: 'sinextension', size: 100 }), 'El adjunto debe ser un archivo XML, PDF, JPG o PNG');
    assert.equal(c.validarAdjunto({ name: 'a.pdf', size: 5 * 1024 * 1024 + 1 }), 'El adjunto excede el tamaño máximo permitido de 5 MB');
    assert.equal(c.validarAdjunto({ name: 'a.pdf', size: 5 * 1024 * 1024 }), null);
    assert.equal(c.validarAdjunto({ name: 'a.pdf', size: 2 * 1024 * 1024 + 1 }, 2 * 1024 * 1024), 'El adjunto excede el tamaño máximo permitido de 2 MB');
    assert.equal(c.validarAdjunto(null), null);
    const e = estadoValido(c);
    assert.ok(c.validar(e, { regla: REGLA_RTS, hoy: '2026-06-20', adjunto: { name: 'a.exe', size: 1 } }).errores.length === 1);
});

test('armarDatos: EFECTIVO sin cuenta bancaria; transferencia con la cuenta como entero; líneas con tarifa del bien, IVA y tratamiento', () => {
    const { c } = cargar();
    const e = estadoValido(c);
    const d = c.armarDatos(e);
    assert.equal(d.proveedorId, 7);
    assert.equal(d.numeroComprobanteProveedor, 'F-100');
    assert.equal(d.fechaComprobante, '2026-06-20');
    assert.equal(d.metodoPago, 'EFECTIVO');
    assert.equal(d.idCuentaBancariaOrigen, null);
    assert.equal(d.lineas.length, 1);
    assert.deepEqual({ ...d.lineas[0], descripcion: undefined }, {
        descripcion: undefined, cuentaContableId: 42, codigoTarifa: '08', base: 1000, iva: 0, tratamiento: 'NO_ACREDITABLE', tarifaLimite: null });
    const t = c.armarDatos({ ...e, metodoPago: 'TRANSFERENCIA', cuentaBancariaId: '3', referencia: ' ref ' });
    assert.equal(t.idCuentaBancariaOrigen, 3);
    assert.equal(t.referencia, 'ref');
    const ef = c.armarDatos({ ...e, metodoPago: 'EFECTIVO', cuentaBancariaId: '3' });
    assert.equal(ef.idCuentaBancariaOrigen, null, 'en EFECTIVO el origen es la caja general');
});

test('armarEnvio: un solo multipart con la parte «datos» JSON y, si hay, el archivo «adjunto»', () => {
    const { c } = cargar();
    const partes = [];
    class FD { append(k, v, n) { partes.push([k, v, n]); } }
    class Bl { constructor(p, o) { this.partes = p; this.type = o.type; } }
    const archivo = { name: 'f.pdf', size: 3 };
    const fd = c.armarEnvio({ a: 1 }, archivo, FD, Bl);
    assert.ok(fd instanceof FD);
    assert.equal(partes[0][0], 'datos');
    assert.equal(partes[0][1].type, 'application/json');
    assert.equal(partes[0][1].partes[0], '{"a":1}');
    assert.deepEqual(partes[1], ['adjunto', archivo, undefined]);
    partes.length = 0;
    c.armarEnvio({ a: 1 }, null, FD, Bl);
    assert.equal(partes.length, 1);
});

test('registrar: una validación fallida no llama al servidor; un envío válido hace un solo POST y confirma con el número de la CxP', async () => {
    const { c } = cargar();
    class FD { constructor() { this.p = []; } append(k, v) { this.p.push(k); } }
    class Bl { constructor(p, o) { this.type = o.type; } }
    let llamadas = [];
    const fetchFn = (url, op) => { llamadas.push([url, op]); return respuesta(true, 200, {
        message: 'Compra de contado registrada: CPP-2026-00007',
        data: { cuentaPorPagarId: 5, numeroCuentaPorPagar: 'CPP-2026-00007', pagoId: 9, numeroPago: 'PAG-9', advertencias: [], tieneAdjunto: false } }); };
    const malo = await c.registrar(fetchFn, c.estadoInicial('2026-06-20', CUENTAS, REGLA_RTS), { regla: REGLA_RTS, hoy: '2026-06-20', FormData: FD, Blob: Bl });
    assert.equal(malo.ok, false);
    assert.ok(malo.errores.length > 0);
    assert.equal(llamadas.length, 0);
    const ok = await c.registrar(fetchFn, estadoValido(c), { regla: REGLA_RTS, hoy: '2026-06-20', FormData: FD, Blob: Bl });
    assert.equal(ok.ok, true);
    assert.equal(llamadas.length, 1);
    assert.equal(llamadas[0][0], '/api/compras/contado');
    assert.equal(llamadas[0][1].method, 'POST');
    assert.deepEqual(llamadas[0][1].body.p, ['datos']);
    assert.equal(ok.mensaje, 'Compra de contado registrada: CPP-2026-00007');
    assert.equal(ok.resultado.numeroCuentaPorPagar, 'CPP-2026-00007');
});

test('registrar: 409 duplicado y 400 muestran el mensaje del servidor; 403 y caída de red, mensajes fijos', async () => {
    const { c } = cargar();
    class FD { append() {} }
    class Bl { constructor(p, o) { this.type = o.type; } }
    const opc = { regla: REGLA_RTS, hoy: '2026-06-20', FormData: FD, Blob: Bl };
    const dup = await c.registrar(() => respuesta(false, 409, { message: 'Ya existe una cuenta por pagar de este proveedor con el comprobante F-100' }), estadoValido(c), opc);
    assert.deepEqual([dup.ok, dup.status, Array.from(dup.errores)], [false, 409, ['Ya existe una cuenta por pagar de este proveedor con el comprobante F-100']]);
    const bad = await c.registrar(() => respuesta(false, 400, { message: MAL }), estadoValido(c), opc);
    assert.deepEqual(Array.from(bad.errores), [MAL]);
    const prohibido = await c.registrar(() => respuesta(false, 403, {}), estadoValido(c), opc);
    assert.deepEqual(Array.from(prohibido.errores), ['No tiene permiso para registrar compras de contado']);
    const red = await c.registrar(() => Promise.reject(new Error('x')), estadoValido(c), opc);
    assert.deepEqual(Array.from(red.errores), ['No se pudo registrar la compra; intente de nuevo']);
});

test('renderConfirmacion y renderErrores: todo con textContent; cadenas hostiles quedan como texto', () => {
    const { c } = cargar();
    const doc = documento();
    const cont = crear('div');
    c.renderConfirmacion(doc, cont, { numeroCuentaPorPagar: MAL, numeroPago: MAL, advertencias: [MAL], tieneAdjunto: true });
    const t = textoDe(cont);
    assert.ok(t.includes('Compra de contado registrada: ' + MAL));
    assert.ok(t.includes(MAL));
    let nodos = 0;
    recorrer(cont, (n) => { nodos++; assert.ok(!('tag' in n) || n.tag !== 'img'); });
    assert.ok(nodos > 1);
    c.renderErrores(doc, cont, [MAL, 'otro']);
    assert.equal(cont.hijos.length, 2);
    assert.equal(cont.hijos[0]._texto, MAL);
});

test('proveedores: la búsqueda llama al endpoint existente y se pinta con textContent; sin resultados y con cédula ofrece crearlo', async () => {
    const { c } = cargar();
    let url = null;
    const lista = await c.buscarProveedores((u) => { url = u; return respuesta(true, 200, { data: [{ id: 1, nombre: MAL, cedula: '3101', regimenHacienda: 'GENERAL' }] }); }, ' Mon rachem ');
    assert.equal(url, '/api/proveedores/buscar?q=' + encodeURIComponent('Mon rachem'));
    assert.equal(lista.proveedores.length, 1);
    const doc = documento();
    const cont = crear('div');
    let elegido = null;
    c.renderProveedores(doc, cont, lista.proveedores, '3101', (p) => { elegido = p; });
    assert.ok(textoDe(cont).includes(MAL));
    const botones = [];
    recorrer(cont, (n) => { if (n.tag === 'button') botones.push(n); });
    botones[0].listeners.click();
    assert.equal(elegido.id, 1);
    // sin resultados y texto con forma de cédula: acción de crear con esa cédula
    const vacio = crear('div');
    let pedido = null;
    c.renderProveedores(doc, vacio, [], '3-101-123456', null, (ced) => { pedido = ced; });
    const crearBtn = [];
    recorrer(vacio, (n) => { if (n.tag === 'button') crearBtn.push(n); });
    assert.equal(crearBtn.length, 1);
    assert.ok(textoDe(vacio).includes('3101123456'));
    crearBtn[0].listeners.click();
    assert.equal(pedido, '3101123456');
    // sin forma de cédula: solo el aviso
    const nombre = crear('div');
    c.renderProveedores(doc, nombre, [], 'Monrachem', null, () => {});
    const sinBoton = [];
    recorrer(nombre, (n) => { if (n.tag === 'button') sinBoton.push(n); });
    assert.equal(sinBoton.length, 0);
    const fallo = await c.buscarProveedores(() => respuesta(false, 403, { message: 'x' }), 'abc');
    assert.equal(fallo.error, 'No tiene permiso para buscar proveedores');
});

test('crearProveedor: valida cédula y nombre, hace POST /api/proveedores y devuelve el régimen que informó Hacienda', async () => {
    const { c } = cargar();
    assert.equal((await c.crearProveedor(() => assert.fail('no debe llamar'), '12', 'X')).error, 'La cédula debe tener entre 9 y 12 dígitos');
    assert.equal((await c.crearProveedor(() => assert.fail('no debe llamar'), '3101123456', '  ')).error, 'El nombre del proveedor es obligatorio');
    let llamada = null;
    const r = await c.crearProveedor((u, op) => { llamada = [u, op]; return respuesta(true, 200, { data: { id: 9, nombre: 'Nuevo', cedula: '3101123456', regimenHacienda: 'SIMPLIFICADO' } }); }, '3-101-123456', ' Nuevo ');
    assert.equal(llamada[0], '/api/proveedores');
    assert.equal(llamada[1].method, 'POST');
    const cuerpo = JSON.parse(llamada[1].body);
    assert.equal(cuerpo.cedula, '3101123456');
    assert.equal(cuerpo.nombre, 'Nuevo');
    assert.equal(cuerpo.tipoProveedor, 'NACIONAL');
    assert.equal(r.proveedor.regimenHacienda, 'SIMPLIFICADO');
    const sinPermiso = await c.crearProveedor(() => respuesta(false, 403, {}), '3101123456', 'N');
    assert.equal(sinPermiso.error, 'No tiene permiso para crear proveedores');
    const dup = await c.crearProveedor(() => respuesta(false, 409, { message: 'Ya existe un proveedor con la cédula 3101123456' }), '3101123456', 'N');
    assert.equal(dup.error, 'Ya existe un proveedor con la cédula 3101123456');
});

test('renderProveedorElegido: nombre, cédula y régimen en texto; SIN_VERIFICAR se explica', () => {
    const { c } = cargar();
    const doc = documento();
    const cont = crear('div');
    c.renderProveedorElegido(doc, cont, { id: 1, nombre: MAL, cedula: '3101', regimenHacienda: 'SIMPLIFICADO' });
    assert.ok(textoDe(cont).includes(MAL));
    assert.ok(textoDe(cont).includes('Régimen simplificado'));
    c.renderProveedorElegido(doc, cont, { id: 1, nombre: 'A', cedula: '3', regimenHacienda: 'SIN_VERIFICAR' });
    assert.ok(textoDe(cont).includes('Sin verificar'));
    c.renderProveedorElegido(doc, cont, null);
    assert.equal(textoDe(cont), '');
});

test('renderDetalle de la CxP: con adjunto muestra un enlace de descarga a /api/cuentas-pagar/{id}/adjunto con el nombre como texto', () => {
    const { l } = cargar();
    const doc = documento();
    const cont = crear('div');
    l.renderDetalle(doc, cont, { id: 15, numero: 'CPP-1', moneda: 'CRC', monto: 1, saldoPendiente: 0, lineas: [], adjuntoNombre: MAL });
    const enlaces = [];
    recorrer(cont, (n) => { if (n.tag === 'a') enlaces.push(n); });
    assert.equal(enlaces.length, 1);
    assert.equal(enlaces[0].attrs.href, '/api/cuentas-pagar/15/adjunto');
    assert.equal(enlaces[0]._texto, MAL);
    const sin = crear('div');
    l.renderDetalle(doc, sin, { id: 16, numero: 'CPP-2', moneda: 'CRC', monto: 1, saldoPendiente: 0, lineas: [] });
    let hay = false;
    recorrer(sin, (n) => { if (n.tag === 'a') hay = true; });
    assert.equal(hay, false);
});

test('ningún innerHTML, outerHTML ni insertAdjacentHTML en compra-contado.js, en su pantalla ni en la plantilla', () => {
    for (const f of [COMPRA, PAGINA, PLANTILLA]) {
        const fuente = fs.readFileSync(f, 'utf8');
        assert.ok(!/innerHTML|outerHTML|insertAdjacentHTML|document\.write/.test(fuente), f);
    }
});
