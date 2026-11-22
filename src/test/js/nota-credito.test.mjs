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
    const motivo = 'La factura ya fue anulada';
    // T-048: la nota de crédito sobre facturas con pagos ya no se bloquea; la constante obsoleta no existe
    assert.equal(n.MOTIVO_PAGOS, undefined);
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

test('T-046 código 13: texto de efecto con el mes de la factura original (ADR-050)', () => {
    const n = cargar();
    assert.equal(n.textoEfecto('13', '2026-09'),
        'El asiento se registra con la fecha de la nota; el IVA se atribuye al mes de la factura original (2026-09)');
    // sin mes del servidor: el texto sigue siendo correcto, sin paréntesis
    assert.equal(n.textoEfecto('13'),
        'El asiento se registra con la fecha de la nota; el IVA se atribuye al mes de la factura original');
    assert.equal(n.textoEfecto('13', 'basura'), n.textoEfecto('13'));
});

test('T-046 advertencia fija del 13 sobre una factura del mismo mes (hora de Costa Rica), no bloqueante', () => {
    const n = cargar();
    const AVISO = 'La factura es de este mismo mes: el código 01 tiene el mismo efecto';
    const ahora = new Date('2026-11-19T20:00:00-06:00');
    assert.equal(n.mesActual(ahora), '2026-11');
    assert.equal(n.advertenciaMismoMes('13', '2026-11', ahora), AVISO);
    assert.equal(n.ADVERTENCIA_MISMO_MES, AVISO);
    assert.equal(n.advertenciaMismoMes('13', '2026-10', ahora), '');
    assert.equal(n.advertenciaMismoMes('01', '2026-11', ahora), '');
    assert.equal(n.advertenciaMismoMes('13', null, ahora), '');
    // el mes se toma en hora de Costa Rica: 2026-12-01 03:00 UTC sigue siendo noviembre allá
    assert.equal(n.mesActual(new Date('2026-12-01T03:00:00Z')), '2026-11');
    assert.equal(n.advertenciaMismoMes('13', '2026-11', new Date('2026-12-01T03:00:00Z')), AVISO);
});

test('T-046 fecha contable sin desfase de zona horaria', () => {
    const n = cargar();
    assert.equal(n.formatearDia('2026-11-12'), '12/11/2026');
    assert.equal(n.formatearDia(null), '-');
});

test('cuerpo del pedido y texto del efecto por código', () => {
    const n = cargar();
    assert.equal(JSON.stringify(n.cuerpoPedido(7, '13', '  Error material ')),
        '{"idFactura":7,"tipo":"CREDITO","codigoReferencia":"13","razon":"Error material"}');
    assert.match(n.textoEfecto('01'), /fecha de la nota; el IVA se atribuye al mes de la nota/);
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
    assert.equal(n.queryListado({ estado: 'EN_TRAMITE', ambiente: 'PRODUCCION' }, 0, 20),
        'estado=EN_TRAMITE&ambiente=PRODUCCION&page=0&size=20');
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

// ---- T-047 nota parcial (ADR-051): total en vivo, topes, pedido y pintado ----

const LINEAS = [
    { idLineaFactura: 11, numeroLinea: 1, descripcion: 'Arroz', total: 113.00, acreditado: 0, disponible: 113.00 },
    { idLineaFactura: 12, numeroLinea: 2, descripcion: 'Frijol', total: 50.00, acreditado: 20.00, disponible: 30.00 }
];

test('T-047/T-051 códigos por modalidad: total 01/06/13, parcial 02/06/14, con su texto de efecto (ADR-050)', () => {
    const n = cargar();
    assert.equal(n.codigosDe('TOTAL').map((c) => c.codigo).join(), '01,06,13');
    assert.equal(n.codigosDe('PARCIAL').map((c) => c.codigo).join(), '02,06,14');
    assert.match(n.textoEfecto('02'), /fecha de la nota; el IVA se atribuye al mes de la nota/);
    assert.equal(n.textoEfecto('14', '2026-09'),
        'El asiento se registra con la fecha de la nota; el IVA se atribuye al mes de la factura original (2026-09)');
    assert.equal(n.nombreCodigo('02'), 'Corrige monto');
    assert.equal(n.nombreCodigo('14'), 'Corrige monto por error material');
});

const EFECTO_06 = 'Devolución de mercancía: afecta el período de la nota. El sistema no controla inventario, '
    + 'así que no se reingresa stock (ADR-061)';

test('T-051 el 06 se ofrece en Total y en Parcial con su texto de efecto (sin inventario)', () => {
    const n = cargar();
    for (const modalidad of ['TOTAL', 'PARCIAL']) {
        const c06 = n.codigosDe(modalidad).filter((c) => c.codigo === '06');
        assert.equal(c06.length, 1, modalidad);
        assert.equal(c06[0].nombre, 'Devolución de mercancía');
    }
    assert.equal(n.textoEfecto('06'), EFECTO_06);
    // el período original no cambia el texto: el 06 siempre atribuye al mes de la nota
    assert.equal(n.textoEfecto('06', '2026-09'), EFECTO_06);
    assert.equal(n.nombreCodigo('06'), 'Devolución de mercancía');
    assert.equal(n.advertenciaMismoMes('06', '2026-11', new Date('2026-11-15T12:00:00Z')), '');
    // 01, 02, 13 y 14 siguen en su modalidad
    assert.equal(n.codigosDe('TOTAL').filter((c) => c.codigo !== '06').map((c) => c.codigo).join(), '01,13');
    assert.equal(n.codigosDe('PARCIAL').filter((c) => c.codigo !== '06').map((c) => c.codigo).join(), '02,14');
});

test('T-051 el pedido del 06: sin líneas (total) no lleva `lineas`; con líneas (parcial) sí', () => {
    const n = cargar();
    assert.equal(n.cuerpoPedido(7, '06', ' Devolución ').lineas, undefined);
    assert.equal(n.cuerpoPedido(7, '06', 'Devolución', []).lineas, undefined);
    const cuerpo = n.cuerpoPedido(7, '06', 'Devolución', [{ idLineaFactura: 11, montoAcreditar: '10.00' }]);
    assert.equal(cuerpo.codigoReferencia, '06');
    assert.equal(cuerpo.lineas.length, 1);
});

test('T-051 el listado muestra «06 - Devolución de mercancía» con la modalidad persistida (Total o Parcial)', () => {
    const n = cargar();
    for (const parcial of [false, true]) {
        const doc = documentoFalso();
        const tr = n.filaNota(doc, { id: 5, numero: 'NC-5', idFactura: 9, codigoReferencia: '06',
            codigoNombre: 'Devolución de mercancía', parcial, total: 10, moneda: 'CRC' }, {});
        const tdCodigo = tr.hijos[3];
        assert.equal(tdCodigo._texto, '06 - Devolución de mercancía');
        assert.equal(tdCodigo.hijos[0]._texto, parcial ? 'Parcial' : 'Total');
    }
});

test('T-047 advertencia del 14 sobre una factura del mismo mes; el 02 no la lleva', () => {
    const n = cargar();
    const ahora = new Date('2026-11-15T12:00:00Z');
    assert.match(n.advertenciaMismoMes('14', '2026-11', ahora), /mismo mes: el código 02 tiene el mismo efecto/);
    assert.equal(n.advertenciaMismoMes('14', '2026-10', ahora), '');
    assert.equal(n.advertenciaMismoMes('02', '2026-11', ahora), '');
    assert.equal(n.advertenciaMismoMes('13', '2026-11', ahora), n.ADVERTENCIA_MISMO_MES);
});

test('T-047 monto por línea: vacío no cuenta; dos decimales; límite exacto ok, +0.01 rechazado', () => {
    const n = cargar();
    assert.equal(n.validarMontoLinea('', 30), null);
    assert.equal(n.validarMontoLinea('30.00', 30), null);
    assert.equal(n.validarMontoLinea('30', 30), null);
    assert.match(n.validarMontoLinea('30.01', 30), /supera el disponible de la línea/);
    assert.match(n.validarMontoLinea('0', 30), /mayor que 0/);
    assert.match(n.validarMontoLinea('-1', 30), /mayor que 0/);
    assert.match(n.validarMontoLinea('1.234', 30), /dos decimales/);
    assert.match(n.validarMontoLinea('abc', 30), /número/);
});

test('T-047 total en vivo: suma en céntimos sin deriva de coma flotante', () => {
    const n = cargar();
    const r = n.calcularParcial(LINEAS, { 11: '0.10', 12: '0.20' }, 143);
    assert.equal(r.total, '0.30');
    assert.equal(r.valido, true);
    assert.deepEqual(JSON.parse(JSON.stringify(r.lineas)),
        [{ idLineaFactura: 11, montoAcreditar: 0.1 }, { idLineaFactura: 12, montoAcreditar: 0.2 }]);
    assert.equal(n.calcularParcial(LINEAS, {}, 143).total, '0.00');
});

test('T-047 el tope de la línea y el de la factura se validan; el límite exacto pasa y un céntimo más no', () => {
    const n = cargar();
    const ok = n.calcularParcial(LINEAS, { 11: '113.00', 12: '30.00' }, 143);
    assert.equal(ok.valido, true);
    assert.equal(ok.total, '143.00');
    const linea = n.calcularParcial(LINEAS, { 12: '30.01' }, 143);
    assert.equal(linea.valido, false);
    assert.match(linea.errores[12], /supera el disponible/);
    // tope de factura menor que la suma de las líneas
    const fac = n.calcularParcial(LINEAS, { 11: '100.00', 12: '30.00' }, 129.99);
    assert.equal(fac.valido, false);
    assert.match(fac.errorGeneral, /supera el disponible de la factura/);
    assert.equal(n.calcularParcial(LINEAS, { 11: '100.00', 12: '30.00' }, 130).valido, true);
});

test('T-047 sin ninguna línea con monto no es válido (la nota necesita al menos una línea)', () => {
    const n = cargar();
    const r = n.calcularParcial(LINEAS, { 11: '', 12: '' }, 143);
    assert.equal(r.valido, false);
    assert.equal(r.lineas.length, 0);
});

test('T-047 pedido parcial lleva lineas; el total no las lleva', () => {
    const n = cargar();
    assert.equal(JSON.stringify(n.cuerpoPedido(7, '02', ' Precio mal ', [{ idLineaFactura: 11, montoAcreditar: 10.5 }])),
        '{"idFactura":7,"tipo":"CREDITO","codigoReferencia":"02","razon":"Precio mal",'
        + '"lineas":[{"idLineaFactura":11,"montoAcreditar":10.5}]}');
    assert.equal(JSON.stringify(n.cuerpoPedido(7, '01', 'Anula')),
        '{"idFactura":7,"tipo":"CREDITO","codigoReferencia":"01","razon":"Anula"}');
});

test('T-047 el error del servidor (409 de tope) se muestra tal cual, sin cerrar el modal', () => {
    const n = cargar();
    assert.equal(n.mensajeDeError(409, { message: 'El monto a acreditar supera el disponible de la línea' }),
        'El monto a acreditar supera el disponible de la línea');
});

test('T-047 modalidad de la nota en listado y detalle: Total o Parcial', () => {
    const n = cargar();
    assert.equal(n.textoModalidad({ parcial: true }), 'Parcial');
    assert.equal(n.textoModalidad({ parcial: false }), 'Total');
    assert.equal(n.textoModalidad({}), 'Total');
});

test('T-047 la tabla de líneas pinta datos hostiles con textContent y el campo de monto', () => {
    const n = cargar();
    const mal = '<img src=x onerror=alert(1)>';
    const doc = documentoFalso();
    const cuerpo = doc.createElement('tbody');
    n.pintarLineasParcial(doc, cuerpo, [{ ...LINEAS[0], descripcion: mal }, LINEAS[1]], 'CRC', () => {});
    assert.equal(cuerpo.hijos.length, 2);
    const fila = cuerpo.hijos[0];
    assert.equal(fila.hijos[1]._texto, mal);
    assert.equal(fila.hijos[2]._texto, '₡113.00');
    assert.equal(fila.hijos[3]._texto, '₡0.00');
    assert.equal(fila.hijos[4]._texto, '₡113.00');
    const input = fila.hijos[5].hijos[0];
    assert.equal(input.tag, 'input');
    assert.equal(input.attrs['data-id-linea'], '11');
    assert.equal(cuerpo.hijos[1].hijos[3]._texto, '₡20.00');
    assert.equal(cuerpo.hijos[1].hijos[4]._texto, '₡30.00');
});

test('T-047 el modal pinta con textContent y no usa innerHTML (inspección del fuente)', () => {
    const codigo = fs.readFileSync(path.join(JS, 'nota-credito-modal.js'), 'utf8');
    assert.match(codigo, /pintarLineasParcial/);
    assert.match(codigo, /textContent/);
    assert.doesNotMatch(codigo, /\.(innerHTML|outerHTML)\s*[+\-]?=/);
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
