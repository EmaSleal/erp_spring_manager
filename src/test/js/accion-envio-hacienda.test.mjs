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
