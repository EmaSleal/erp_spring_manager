// T-043 criterio 5 y 9: el pintado de los contadores del panel de Facturación (neutro en 0, alerta > 0),
// solo con classList/textContent, nunca innerHTML.
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CODIGO = fs.readFileSync(path.join(raiz, 'src/main/resources/static/modules/facturacion/js/inicio.js'), 'utf8');

function clases() {
    const set = new Set();
    return {
        set,
        add: (...c) => c.forEach((x) => set.add(x)),
        remove: (...c) => c.forEach((x) => set.delete(x)),
        contains: (c) => set.has(c)
    };
}

function tarjeta(texto) {
    const numero = { textContent: texto };
    const el = { classList: clases(), querySelector: (sel) => (sel === '[data-contador]' ? numero : null) };
    Object.defineProperty(el, 'innerHTML', { set() { throw new Error('innerHTML no se debe usar'); } });
    return el;
}

function cargar(tarjetas = []) {
    const ctx = {
        window: {},
        document: {
            addEventListener() {},
            querySelectorAll: (sel) => (sel === '[data-contador-card]' ? tarjetas : [])
        }
    };
    ctx.window.document = ctx.document;
    vm.createContext(ctx);
    vm.runInContext(CODIGO, ctx);
    return ctx.window.PanelInicio;
}

test('estadoContador: 0 es neutro; mayor que 0 es alerta', () => {
    const p = cargar();
    assert.equal(p.estadoContador('0'), 'neutro');
    assert.equal(p.estadoContador(0), 'neutro');
    assert.equal(p.estadoContador('1'), 'alerta');
    assert.equal(p.estadoContador(' 25 '), 'alerta');
});

test('estadoContador: valores no numéricos o vacíos son neutros', () => {
    const p = cargar();
    for (const v of [null, undefined, '', 'abc', '-3', NaN]) {
        assert.equal(p.estadoContador(v), 'neutro', String(v));
    }
});

test('pintarContadores aplica solo la clase que corresponde a cada tarjeta, sin innerHTML', () => {
    const a = tarjeta('0');
    const b = tarjeta('7');
    const p = cargar([a, b]);

    assert.deepEqual(Array.from(p.pintarContadores(p === null ? null : { querySelectorAll: () => [a, b] })), ['neutro', 'alerta']);
    assert.ok(a.classList.contains('contador-neutro') && !a.classList.contains('contador-alerta'));
    assert.ok(b.classList.contains('contador-alerta') && !b.classList.contains('contador-neutro'));
});

test('repintar cuando el número cambia quita la clase anterior', () => {
    const a = tarjeta('3');
    const p = cargar([a]);
    p.pintarTarjeta(a);
    assert.ok(a.classList.contains('contador-alerta'));
    a.querySelector('[data-contador]').textContent = '0';
    p.pintarTarjeta(a);
    assert.ok(a.classList.contains('contador-neutro') && !a.classList.contains('contador-alerta'));
});

test('el código del panel no usa innerHTML', () => {
    assert.ok(!CODIGO.includes('innerHTML'));
});
