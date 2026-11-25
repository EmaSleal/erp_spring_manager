// T-056 criterios 4 y 10: validaciones del formulario de régimen tributario y pintado del historial solo con
// createElement/textContent (nunca innerHTML con datos del servidor).
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const RUTA = 'src/main/resources/static/modules/configuracion/js/regimen-tributario.js';
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

const HOY = '2026-11-25';

test('validarTramo: el régimen es obligatorio y solo GENERAL o SIMPLIFICADO', () => {
    const { RegimenTributarioUI: u } = cargar().window;
    assert.match(u.validarTramo({ regimen: '', vigenteDesde: '2027-01-01', nota: '' }, HOY), /régimen/i);
    assert.match(u.validarTramo({ regimen: 'OTRO', vigenteDesde: '2027-01-01', nota: '' }, HOY), /régimen/i);
    assert.equal(u.validarTramo({ regimen: 'GENERAL', vigenteDesde: '2027-01-01', nota: '' }, HOY), null);
    assert.equal(u.validarTramo({ regimen: 'SIMPLIFICADO', vigenteDesde: '2027-01-01', nota: 'n' }, HOY), null);
});

test('validarTramo: la fecha es obligatoria, válida y posterior a hoy', () => {
    const { RegimenTributarioUI: u } = cargar().window;
    for (const f of ['', null, undefined, 'abc', '2027-13-01', '2027-02-30', '01/01/2027']) {
        assert.match(u.validarTramo({ regimen: 'GENERAL', vigenteDesde: f, nota: '' }, HOY), /fecha/i, String(f));
    }
    assert.match(u.validarTramo({ regimen: 'GENERAL', vigenteDesde: HOY, nota: '' }, HOY), /posterior a hoy/i);
    assert.match(u.validarTramo({ regimen: 'GENERAL', vigenteDesde: '2026-11-24', nota: '' }, HOY), /posterior a hoy/i);
    assert.equal(u.validarTramo({ regimen: 'GENERAL', vigenteDesde: '2026-11-26', nota: '' }, HOY), null);
});

test('validarTramo: la nota admite hasta 300 caracteres', () => {
    const { RegimenTributarioUI: u } = cargar().window;
    assert.equal(u.validarTramo({ regimen: 'GENERAL', vigenteDesde: '2027-01-01', nota: 'x'.repeat(300) }, HOY), null);
    assert.match(u.validarTramo({ regimen: 'GENERAL', vigenteDesde: '2027-01-01', nota: 'x'.repeat(301) }, HOY), /300/);
});

test('etiquetaRegimen y estadoTramo', () => {
    const { RegimenTributarioUI: u } = cargar().window;
    assert.equal(u.etiquetaRegimen('GENERAL'), 'Régimen general');
    assert.equal(u.etiquetaRegimen('SIMPLIFICADO'), 'Régimen simplificado');
    assert.equal(u.etiquetaRegimen('<img src=x>'), '<img src=x>');
    assert.equal(u.estadoTramo({ vigente: true, proximo: false }), 'Vigente hoy');
    assert.equal(u.estadoTramo({ vigente: false, proximo: true }), 'Próximo');
    assert.equal(u.estadoTramo({ vigente: false, proximo: false, vigenteDesde: '2026-01-01' }, HOY), 'Anterior');
    assert.equal(u.estadoTramo({ vigente: false, proximo: false, vigenteDesde: '2029-01-01' }, HOY), 'Futuro');
});

test('pintarHistorial pinta con textContent: un dato con HTML queda como texto y no se usa innerHTML', () => {
    const ctx = cargar();
    const u = ctx.window.RegimenTributarioUI;
    const cuerpo = elemento('tbody');
    const malicioso = '<script>alert(1)</script>';
    u.pintarHistorial(cuerpo, [
        { id: 1, regimen: 'SIMPLIFICADO', vigenteDesde: '2000-01-01', nota: malicioso, usuarioNombre: malicioso, vigente: false, proximo: false, editable: false, bloqueo: 'x' },
        { id: 2, regimen: 'GENERAL', vigenteDesde: '2026-09-01', nota: 'n', usuarioNombre: null, vigente: true, proximo: false, editable: false },
        { id: 3, regimen: 'SIMPLIFICADO', vigenteDesde: '2027-01-01', nota: '', vigente: false, proximo: true, editable: true }
    ], { puedeGestionar: true, onEditar() {}, onEliminar() {} });
    assert.equal(cuerpo.children.length, 3);
    const textos = cuerpo.children[0].children.map((td) => td.textContent);
    assert.ok(textos.includes(malicioso), 'el texto con HTML se muestra tal cual');
    assert.ok(cuerpo.children[1].children.some((td) => td.textContent === 'Vigente hoy'));
    assert.ok(cuerpo.children[2].children.some((td) => td.textContent === 'Próximo'));
});

test('pintarHistorial: los botones de editar y borrar solo salen en tramos editables y con permiso', () => {
    const u = cargar().window.RegimenTributarioUI;
    const contar = (fila) => fila.children.flatMap((td) => td.children).filter((b) => b.tag === 'button').length;
    const lista = [
        { id: 2, regimen: 'GENERAL', vigenteDesde: '2026-09-01', vigente: true, editable: false },
        { id: 3, regimen: 'SIMPLIFICADO', vigenteDesde: '2027-01-01', proximo: true, editable: true }
    ];
    const llamadas = [];
    const cuerpo = elemento('tbody');
    u.pintarHistorial(cuerpo, lista, { puedeGestionar: true, onEditar: (t) => llamadas.push(['e', t.id]), onEliminar: (t) => llamadas.push(['d', t.id]) });
    assert.equal(contar(cuerpo.children[0]), 0);
    assert.equal(contar(cuerpo.children[1]), 2);
    const botones = cuerpo.children[1].children.flatMap((td) => td.children).filter((b) => b.tag === 'button');
    botones[0].listeners.click();
    botones[1].listeners.click();
    assert.deepEqual(llamadas, [['e', 3], ['d', 3]]);

    const solo = elemento('tbody');
    u.pintarHistorial(solo, lista, { puedeGestionar: false });
    assert.equal(contar(solo.children[1]), 0);
});

test('pintarHistorial: sin tramos muestra un mensaje en una sola fila', () => {
    const u = cargar().window.RegimenTributarioUI;
    const cuerpo = elemento('tbody');
    u.pintarHistorial(cuerpo, [], { puedeGestionar: true });
    assert.equal(cuerpo.children.length, 1);
    assert.match(cuerpo.children[0].children[0].textContent, /No hay tramos/);
});

test('el código nunca asigna innerHTML ni usa insertAdjacentHTML/outerHTML', () => {
    assert.doesNotMatch(CODIGO, /\.innerHTML\s*=/);
    assert.doesNotMatch(CODIGO, /insertAdjacentHTML|outerHTML|document\.write/);
});
