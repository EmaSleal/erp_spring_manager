// T-038 criterio 19: actividades de «Verificar en Hacienda»: pintado con textContent, preselección y copia al campo.
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CODIGO = fs.readFileSync(path.join(raiz, 'src/main/resources/static/modules/cliente/js/cliente-hacienda.js'), 'utf8');
const PAYLOAD = '<img src=x onerror=alert(1)>';

function entorno() {
    const registro = { innerHTML: [] };
    function nodo(tag) {
        const n = {
            tag, hijos: [], _text: '', style: {}, checked: false, value: '',
            appendChild(h) { this.hijos.push(h); return h; },
            set textContent(v) { this._text = String(v); this.hijos = []; },
            get textContent() { return this._text; },
            set innerHTML(v) { registro.innerHTML.push(String(v)); },
            querySelectorAll(sel) {
                const radios = [];
                (function rec(x) { x.hijos.forEach(h => { if (h.tag === 'input') radios.push(h); rec(h); }); })(this);
                return sel === 'input[type="radio"]' ? radios : [];
            }
        };
        return n;
    }
    const doc = { createElement: nodo };
    const ctx = { document: doc };
    vm.createContext(ctx);
    vm.runInContext(CODIGO, ctx);
    return { ctx, contenedor: nodo('div'), campo: nodo('input'), registro };
}

const radios = c => c.querySelectorAll('input[type="radio"]');

test('sin actividades: mensaje de ausencia, nada que elegir ni copiar', () => {
    const { ctx, contenedor, campo } = entorno();
    assert.equal(ctx.pintarActividadesHacienda(contenedor, []), 0);
    assert.equal(ctx.pintarActividadesHacienda(contenedor, undefined), 0);
    assert.equal(radios(contenedor).length, 0);
    assert.match(contenedor.hijos[0].textContent, /no informa actividades/);
    assert.equal(ctx.copiarActividadHacienda(contenedor, campo), false);
    assert.equal(campo.value, '');
});

test('una sola actividad activa se preselecciona y se copia tal cual (4773.0)', () => {
    const { ctx, contenedor, campo } = entorno();
    assert.equal(ctx.pintarActividadesHacienda(contenedor, [
        { codigo: '4773.0', descripcion: 'Venta', estado: 'A', tipo: 'P' },
        { codigo: '620100', descripcion: 'Otra', estado: 'I', tipo: 'S' }]), 2);
    const r = radios(contenedor);
    assert.equal(r[0].checked, true);
    assert.equal(r[1].checked, false);
    assert.equal(ctx.actividadHaciendaElegida(contenedor), '4773.0');
    assert.equal(ctx.copiarActividadHacienda(contenedor, campo), true);
    assert.equal(campo.value, '4773.0');
    assert.match(contenedor.hijos[0].hijos[1].textContent, /4773\.0 - Venta \(Principal, Activa\)/);
});

test('varias activas: ninguna preseleccionada; copiar sin elegir no toca el campo; elegida se copia', () => {
    const { ctx, contenedor, campo } = entorno();
    campo.value = 'previo';
    ctx.pintarActividadesHacienda(contenedor, [
        { codigo: '4773.0', estado: 'A', tipo: 'P' }, { codigo: '477300', estado: 'A', tipo: 'S' }]);
    assert.equal(ctx.actividadHaciendaElegida(contenedor), null);
    assert.equal(ctx.copiarActividadHacienda(contenedor, campo), false);
    assert.equal(campo.value, 'previo');
    radios(contenedor)[1].checked = true;
    assert.equal(ctx.copiarActividadHacienda(contenedor, campo), true);
    assert.equal(campo.value, '477300');
});

test('el texto del servidor va con textContent: innerHTML nunca se toca; se descartan entradas sin código', () => {
    const { ctx, contenedor, registro } = entorno();
    const n = ctx.pintarActividadesHacienda(contenedor, [
        { codigo: '4773.0', descripcion: PAYLOAD, estado: 'A', tipo: 'P' }, null, { descripcion: 'sin codigo' }]);
    assert.equal(n, 1);
    assert.ok(contenedor.hijos[0].hijos[1].textContent.includes(PAYLOAD));
    assert.equal(registro.innerHTML.length, 0);
});
