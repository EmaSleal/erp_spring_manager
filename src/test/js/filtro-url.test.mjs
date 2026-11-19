// T-043: filtros por URL que el panel pasa a los listados (?estado=X&vigente=true).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CODIGO = fs.readFileSync(path.join(raiz, 'src/main/resources/static/modules/facturacion/js/filtro-url.js'), 'utf8');
const plano = (v) => JSON.parse(JSON.stringify(v));

function cargar() {
    const ctx = { window: {}, URLSearchParams };
    vm.createContext(ctx);
    vm.runInContext(CODIGO, ctx);
    return ctx.window.FiltroUrl;
}

test('leerFiltros: estado, vigente y ambiente de la URL', () => {
    const f = cargar();
    assert.deepEqual(plano(f.leerFiltros('?estado=ERROR&vigente=true&ambiente=PRODUCCION')),
        { estado: 'ERROR', vigente: true, ambiente: 'PRODUCCION' });
    assert.deepEqual(plano(f.leerFiltros('?estado=ENVIO_INCIERTO')),
        { estado: 'ENVIO_INCIERTO', vigente: false, ambiente: '' });
    assert.deepEqual(plano(f.leerFiltros('?vigente=false')), { estado: '', vigente: false, ambiente: '' });
});

test('leerFiltros: sin parámetros o con valores que no tienen forma de nombre quedan vacíos', () => {
    const f = cargar();
    for (const s of ['', undefined, null, '?estado=', '?estado=error', "?estado=<script>", '?estado=A/B']) {
        assert.equal(f.leerFiltros(s).estado, '', String(s));
    }
    for (const s of ['?ambiente=sandbox', '?ambiente=<b>', '?ambiente=A B']) {
        assert.equal(f.leerFiltros(s).ambiente, '', String(s));
    }
});

test('el filtro de vigentes ya no se hace en el cliente: no existe soloVigentes', () => {
    assert.equal(cargar().soloVigentes, undefined);
});

test('queryServidor: página, tamaño y solo los filtros con valor', () => {
    const f = cargar();
    assert.equal(f.queryServidor({ vigente: true, ambiente: 'SANDBOX' }, 1, 100),
        'page=1&size=100&vigente=true&ambiente=SANDBOX');
    assert.equal(f.queryServidor({ vigente: false, ambiente: '' }, 0, 100), 'page=0&size=100');
    assert.equal(f.queryServidor(undefined), 'page=0&size=100');
});

test('hayFiltroServidor y textoFiltro', () => {
    const f = cargar();
    assert.equal(f.hayFiltroServidor({ vigente: true, ambiente: '' }), true);
    assert.equal(f.hayFiltroServidor({ vigente: false, ambiente: 'PRODUCCION' }), true);
    assert.equal(f.hayFiltroServidor({ vigente: false, ambiente: '' }), false);
    assert.equal(f.textoFiltro({ vigente: true, ambiente: 'PRODUCCION' }), 'Solo vigentes · ambiente PRODUCCION');
    assert.equal(f.textoFiltro({ vigente: true, ambiente: '' }), 'Solo vigentes');
    assert.equal(f.textoFiltro({}), '');
});

test('textoTotal: muestra el total del servidor, también con más de 100 filas', () => {
    const f = cargar();
    assert.equal(f.textoTotal('130', 0, 100, 100), 'Mostrando 1-100 de 130');
    assert.equal(f.textoTotal('130', 1, 100, 30), 'Mostrando 101-130 de 130');
    assert.equal(f.textoTotal('0', 0, 100, 0), 'Sin comprobantes');
    assert.equal(f.textoTotal(null, 0, 100, 5), '');
});
