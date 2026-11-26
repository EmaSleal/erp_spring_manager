// T-040 criterio 8: acción del botón de envío a Hacienda del modal según el estado del comprobante vigente.
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CODIGO = fs.readFileSync(path.join(raiz,
    'src/main/resources/static/modules/facturacion/js/accion-envio-hacienda.js'), 'utf8');

function cargar() {
    const ctx = { window: {} };
    vm.createContext(ctx);
    vm.runInContext(CODIGO, ctx);
    return ctx.window.AccionEnvioHacienda;
}

test('sin comprobante (vacío, null, undefined) → emitir', () => {
    const a = cargar();
    for (const v of [null, undefined, '']) {
        assert.equal(a.accionEnvioHacienda(v), 'emitir');
    }
});

test('RECHAZADO → emitir (reemisión por /procesar)', () => {
    assert.equal(cargar().accionEnvioHacienda('RECHAZADO'), 'emitir');
});

test('NO_TRANSMITIDO y ERROR → reenviar', () => {
    const a = cargar();
    assert.equal(a.accionEnvioHacienda('NO_TRANSMITIDO'), 'reenviar');
    assert.equal(a.accionEnvioHacienda('ERROR'), 'reenviar');
});

test('estados sin botón de envío → null', () => {
    const a = cargar();
    for (const e of ['ENVIO_INCIERTO', 'ENVIADO', 'PROCESANDO', 'ERROR_HACIENDA', 'ACEPTADO',
        'GENERADO', 'FIRMADO', 'ESTADO_DESCONOCIDO']) {
        assert.equal(a.accionEnvioHacienda(e), null, e);
    }
});

test('texto del botón según la acción', () => {
    const a = cargar();
    assert.equal(a.textoBotonEnvio('emitir'), 'Enviar a Hacienda');
    assert.equal(a.textoBotonEnvio('reenviar'), 'Reenviar a Hacienda');
    assert.equal(a.textoBotonEnvio(null), null);
});

// T-057 criterio 11: una factura del régimen simplificado no se emite a Hacienda: el botón se oculta (acción null)
// sea cual sea el estado del comprobante vigente.
test('T-057: régimen SIMPLIFICADO → sin botón de envío en ningún estado', () => {
    const a = cargar();
    for (const e of [null, undefined, '', 'RECHAZADO', 'NO_TRANSMITIDO', 'ERROR', 'GENERADO', 'ACEPTADO']) {
        assert.equal(a.accionEnvioHacienda(e, 'SIMPLIFICADO'), null, String(e));
    }
});

test('T-057: régimen GENERAL o sin régimen en el DTO → misma acción que antes', () => {
    const a = cargar();
    for (const regimen of ['GENERAL', null, undefined]) {
        assert.equal(a.accionEnvioHacienda(null, regimen), 'emitir');
        assert.equal(a.accionEnvioHacienda('RECHAZADO', regimen), 'emitir');
        assert.equal(a.accionEnvioHacienda('ERROR', regimen), 'reenviar');
        assert.equal(a.accionEnvioHacienda('ACEPTADO', regimen), null);
    }
});

// T-057 criterio 15: una factura con la clave de un comprobante emitido fuera del ERP no habilita el botón de emisión
// (tercer argumento: la clave externa; vacío = sin clave).
test('T-057: con clave externa → sin botón de envío en ningún estado', () => {
    const a = cargar();
    const clave = '50628122600310123456700100001010000000011100000001';
    for (const e of [null, undefined, '', 'RECHAZADO', 'NO_TRANSMITIDO', 'ERROR']) {
        assert.equal(a.accionEnvioHacienda(e, 'GENERAL', clave), null, String(e));
    }
});

test('T-057: sin clave externa (null, undefined o vacía) → misma acción que antes', () => {
    const a = cargar();
    for (const clave of [null, undefined, '']) {
        assert.equal(a.accionEnvioHacienda(null, 'GENERAL', clave), 'emitir');
        assert.equal(a.accionEnvioHacienda('ERROR', 'GENERAL', clave), 'reenviar');
    }
});

test('T-057: el formulario de la clave externa solo en GENERAL, no anulada y sin comprobante del ERP distinto de RECHAZADO', () => {
    const a = cargar();
    assert.equal(a.puedeRegistrarClaveExterna(null, 'GENERAL', false), true);
    assert.equal(a.puedeRegistrarClaveExterna(undefined, undefined, undefined), true);
    assert.equal(a.puedeRegistrarClaveExterna('RECHAZADO', 'GENERAL', false), true);
    for (const e of ['ACEPTADO', 'ENVIADO', 'PROCESANDO', 'ERROR', 'NO_TRANSMITIDO', 'GENERADO']) {
        assert.equal(a.puedeRegistrarClaveExterna(e, 'GENERAL', false), false, e);
    }
    assert.equal(a.puedeRegistrarClaveExterna(null, 'SIMPLIFICADO', false), false);
    assert.equal(a.puedeRegistrarClaveExterna(null, 'GENERAL', true), false);
});
