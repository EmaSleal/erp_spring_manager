// T-056 criterios 7 y 10: validaciones del formulario del tope de compras del RTS y pintado de la lista solo con
// createElement/textContent (nunca innerHTML con datos del servidor).
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const RUTA = 'src/main/resources/static/modules/configuracion/js/tope-compras-rts.js';
const CODIGO = fs.readFileSync(path.join(raiz, RUTA), 'utf8');

function elemento(tag) {
    const el = {
        tag,
        children: [],
        attrs: {},
        className: '',
        textContent: '',
        title: '',
        type: '',
        appendChild(h) { el.children.push(h); return h; },
        replaceChildren(...hijos) { el.children = [...hijos]; },
        setAttribute(k, v) { el.attrs[k] = v; },
        addEventListener(ev, fn) { (el.listeners ||= {})[ev] = fn; }
    };
    Object.defineProperty(el, 'innerHTML', { set() { throw new Error('innerHTML no se debe usar'); }, get() { return ''; } });
    return el;
}

function cargar() {
    const ctx = {
        window: {},
        document: { createElement: elemento, addEventListener() {}, getElementById: () => null }
    };
    ctx.window.document = ctx.document;
    vm.createContext(ctx);
    vm.runInContext(CODIGO, ctx);
    return ctx;
}

const valido = { anio: '2026', salarioBase: '462200.00', multiplicador: '', umbralAviso: '', umbralTope: '', nota: '' };

test('validarTope: año entre 2000 y 2100', () => {
    const u = cargar().window.TopeComprasRtsUI;
    assert.equal(u.validarTope(valido), null);
    for (const a of ['', null, 'abc', '1999', '2101', '2026.5']) {
        assert.match(u.validarTope({ ...valido, anio: a }), /año/i, String(a));
    }
    assert.equal(u.validarTope({ ...valido, anio: '2000' }), null);
    assert.equal(u.validarTope({ ...valido, anio: '2100' }), null);
});

test('validarTope: el salario base es obligatorio, mayor que 0 y con hasta 2 decimales', () => {
    const u = cargar().window.TopeComprasRtsUI;
    for (const s of ['', null, 'x', '0', '-5', '1.234']) {
        assert.match(u.validarTope({ ...valido, salarioBase: s }), /salario base/i, String(s));
    }
    assert.equal(u.validarTope({ ...valido, salarioBase: '462200' }), null);
    assert.equal(u.validarTope({ ...valido, salarioBase: '462200.5' }), null);
});

test('validarTope: multiplicador y umbrales son opcionales; si van, deben ser válidos', () => {
    const u = cargar().window.TopeComprasRtsUI;
    assert.equal(u.validarTope({ ...valido, multiplicador: '186' }), null);
    assert.match(u.validarTope({ ...valido, multiplicador: '0' }), /multiplicador/i);
    assert.match(u.validarTope({ ...valido, multiplicador: '-1' }), /multiplicador/i);
    assert.match(u.validarTope({ ...valido, multiplicador: '1.234' }), /multiplicador/i);
    assert.equal(u.validarTope({ ...valido, umbralAviso: '70', umbralTope: '90' }), null);
    assert.match(u.validarTope({ ...valido, umbralAviso: '0' }), /umbral/i);
    assert.match(u.validarTope({ ...valido, umbralTope: '101' }), /umbral/i);
    assert.match(u.validarTope({ ...valido, umbralAviso: '90', umbralTope: '80' }), /menor/i);
    assert.match(u.validarTope({ ...valido, umbralAviso: '80', umbralTope: '80' }), /menor/i);
});

test('validarTope: la nota admite hasta 300 caracteres', () => {
    const u = cargar().window.TopeComprasRtsUI;
    assert.equal(u.validarTope({ ...valido, nota: 'x'.repeat(300) }), null);
    assert.match(u.validarTope({ ...valido, nota: 'x'.repeat(301) }), /300/);
});

test('pintarTopes pinta con textContent: un dato con HTML queda como texto y no se usa innerHTML', () => {
    const u = cargar().window.TopeComprasRtsUI;
    const cuerpo = elemento('tbody');
    const malicioso = '<script>alert(1)</script>';
    u.pintarTopes(cuerpo, [
        { anio: 2026, salarioBase: 462200, multiplicador: 186, tope: 85969200, umbralAviso: 80, umbralTope: 100, nota: malicioso, usuarioNombre: malicioso },
        { anio: 2025, salarioBase: 450000, multiplicador: 186, tope: 83700000, umbralAviso: 80, umbralTope: 100, nota: '', usuarioNombre: null }
    ], { puedeGestionar: true, onEditar() {} });
    assert.equal(cuerpo.children.length, 2);
    const textos = cuerpo.children[0].children.map((td) => td.textContent);
    assert.ok(textos.includes(malicioso));
    assert.ok(textos.includes('2026'));
    assert.ok(textos.some((t) => /85\.969\.200,00/.test(t)), 'el tope formateado: ' + textos.join('|'));
});

test('pintarTopes: el botón de editar llama a onEditar con la fila y solo sale con permiso', () => {
    const u = cargar().window.TopeComprasRtsUI;
    const lista = [{ anio: 2026, salarioBase: 1, multiplicador: 186, tope: 186, umbralAviso: 80, umbralTope: 100 }];
    const llamadas = [];
    const cuerpo = elemento('tbody');
    u.pintarTopes(cuerpo, lista, { puedeGestionar: true, onEditar: (t) => llamadas.push(t.anio) });
    const botones = cuerpo.children[0].children.flatMap((td) => td.children).filter((b) => b.tag === 'button');
    assert.equal(botones.length, 1);
    botones[0].listeners.click();
    assert.deepEqual(llamadas, [2026]);
    const sin = elemento('tbody');
    u.pintarTopes(sin, lista, { puedeGestionar: false });
    assert.equal(sin.children[0].children.flatMap((td) => td.children).filter((b) => b.tag === 'button').length, 0);
});

test('pintarTopes: sin parámetros muestra un mensaje en una sola fila', () => {
    const u = cargar().window.TopeComprasRtsUI;
    const cuerpo = elemento('tbody');
    u.pintarTopes(cuerpo, [], { puedeGestionar: true });
    assert.equal(cuerpo.children.length, 1);
    assert.match(cuerpo.children[0].children[0].textContent, /No hay/);
});

test('pintarEstado: acumulado, tope y porcentaje del año, o «sin configurar», todo con textContent', () => {
    const u = cargar().window.TopeComprasRtsUI;
    const caja = elemento('div');
    u.pintarEstado(caja, { anio: 2026, configurado: true, tope: 85969200, acumulado: 70000000, porcentaje: 81.43, nivel: 'AVISO', mensaje: '<b>x</b>', advertencia: 'Solo ERP' });
    assert.match(caja.textContent, /70\.000\.000,00/);
    assert.match(caja.textContent, /81,43/);
    assert.match(caja.textContent, /Solo ERP/);
    assert.match(caja.className, /alert-warning/);
    u.pintarEstado(caja, { anio: 2027, configurado: false, mensaje: 'Tope de 2027 sin configurar', advertencia: 'Solo ERP' });
    assert.match(caja.textContent, /Tope de 2027 sin configurar/);
});

test('el código nunca asigna innerHTML ni usa insertAdjacentHTML/outerHTML', () => {
    assert.doesNotMatch(CODIGO, /\.innerHTML\s*=/);
    assert.doesNotMatch(CODIGO, /insertAdjacentHTML|outerHTML|document\.write/);
});
