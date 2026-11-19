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

test('leerFiltros: estado y vigente de la URL', () => {
    const f = cargar();
    assert.deepEqual(plano(f.leerFiltros('?estado=ERROR&vigente=true')), { estado: 'ERROR', vigente: true });
    assert.deepEqual(plano(f.leerFiltros('?estado=ENVIO_INCIERTO')), { estado: 'ENVIO_INCIERTO', vigente: false });
    assert.deepEqual(plano(f.leerFiltros('?vigente=false')), { estado: '', vigente: false });
});

test('leerFiltros: sin parámetros o con un estado que no tiene forma de estado queda vacío', () => {
    const f = cargar();
    for (const s of ['', undefined, null, '?estado=', '?estado=error', "?estado=<script>", '?estado=A/B']) {
        assert.equal(f.leerFiltros(s).estado, '', String(s));
    }
});

test('soloVigentes oculta las filas con vigente=false y deja las que no traen la bandera', () => {
    const f = cargar();
    const filas = [{ id: 1, vigente: true }, { id: 2, vigente: false }, { id: 3 }];
    assert.deepEqual(plano(f.soloVigentes(filas, true)).map((x) => x.id), [1, 3]);
    assert.deepEqual(plano(f.soloVigentes(filas, false)).map((x) => x.id), [1, 2, 3]);
    assert.equal(f.soloVigentes(null, true), null);
});
