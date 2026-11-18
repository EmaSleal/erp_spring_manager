// T-037 criterio 18: ENVIO_INCIERTO con fecha_envio de menos de 2 minutos se muestra «Enviando…»; después, «Envío incierto».
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CODIGO = fs.readFileSync(path.join(raiz,
    'src/main/resources/static/modules/facturacion/electronica/js/estado-envio.js'), 'utf8');

function cargar() {
    const ctx = { window: {} };
    vm.createContext(ctx);
    vm.runInContext(CODIGO, ctx);
    return ctx.window.EstadoEnvio;
}

const ENVIO = '2026-11-17T12:00:00';
const ms = Date.parse(ENVIO);

test('ENVIO_INCIERTO de hace 30 s → Enviando…', () => {
    const e = cargar();
    assert.equal(e.etiquetaEnvio('ENVIO_INCIERTO', ENVIO, ms + 30_000), 'Enviando…');
    assert.equal(e.estaEnviando('ENVIO_INCIERTO', ENVIO, ms + 30_000), true);
});

test('ENVIO_INCIERTO de hace 2 min o más → Envío incierto', () => {
    const e = cargar();
    assert.equal(e.etiquetaEnvio('ENVIO_INCIERTO', ENVIO, ms + 120_000), 'Envío incierto');
    assert.equal(e.etiquetaEnvio('ENVIO_INCIERTO', ENVIO, ms + 600_000), 'Envío incierto');
});

test('ENVIO_INCIERTO sin fecha_envio → Envío incierto', () => {
    const e = cargar();
    assert.equal(e.etiquetaEnvio('ENVIO_INCIERTO', null, ms), 'Envío incierto');
});

test('otros estados no son envío en curso', () => {
    const e = cargar();
    for (const estado of ['ENVIADO', 'NO_TRANSMITIDO', 'FIRMADO', 'ACEPTADO']) {
        assert.equal(e.estaEnviando(estado, ENVIO, ms + 1000), false);
        assert.equal(e.etiquetaEnvio(estado, ENVIO, ms + 1000), null);
    }
});

test('comprobantes.html carga estado-envio.js y el badge usa fechaEnvio en el listado y el detalle', () => {
    const html = fs.readFileSync(path.join(raiz,
        'src/main/resources/templates/modules/facturacion/electronica/comprobantes.html'), 'utf8');
    assert.match(html, /modules\/facturacion\/electronica\/js\/estado-envio\.js/);
    assert.equal((html.match(/getEstadoBadge\(comp\.estado, comp\.fechaEnvio\)/g) || []).length, 2);
    assert.match(html, /EstadoEnvio\.estaEnviando\(estado, fechaEnvio\)/);
});
