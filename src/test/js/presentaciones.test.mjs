// T-034 criterio 14 (D5/D6): mantenimiento de presentaciones. La unidad FE solo se pide cuando NO es empacado
// y los nombres se pintan con textContent.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CODIGO = fs.readFileSync(path.join(raiz, 'src/main/resources/static/modules/configuracion/js/presentaciones.js'), 'utf8');
const PAYLOAD = '<img src=x onerror=alert(1)>';

function el(tag, reg) {
    return {
        tagName: String(tag).toUpperCase(), children: [], style: {}, value: '', checked: false, className: '', _t: '',
        set textContent(v) { this._t = String(v); this.children = []; },
        get textContent() { return this._t + this.children.map(c => c.textContent).join(''); },
        set innerHTML(v) { reg.innerHTML.push(String(v)); },
        appendChild(c) { this.children.push(c); return c; },
        replaceChildren(...n) { this.children = [...n]; this._t = ''; },
        addEventListener() {}
    };
}

function navegador() {
    const reg = { creados: [], innerHTML: [], cuerpos: [] };
    const els = {};
    ['presentacionesBody', 'presentacionesError', 'presentacionModalError', 'presentacionModalTitle', 'presentacionId',
        'presentacionNombre', 'presentacionEmpacado', 'divUnidadFe', 'presentacionUnidadFe']
        .forEach(id => { els[id] = el('div', reg); });
    let respuesta = { status: 200, cuerpo: { success: true, data: {} } };
    const ctx = {
        console,
        document: { getElementById: id => els[id], createElement(t) { reg.creados.push(t); return el(t, reg); }, addEventListener() {} },
        fetch: async (url, init) => {
            reg.cuerpos.push({ url, init });
            return { ok: respuesta.status < 300, status: respuesta.status, json: async () => respuesta.cuerpo };
        },
        encodeURIComponent
    };
    vm.createContext(ctx);
    vm.runInContext(CODIGO + '\n;presModal = { show() {}, hide() {} };', ctx);
    return { ctx, els, reg, responder: r => { respuesta = r; } };
}

test('nombre con HTML se muestra como texto; no hay img ni innerHTML', () => {
    const { ctx, els, reg } = navegador();
    ctx.renderPresentaciones([{ idPresentacion: 1, nombre: PAYLOAD, empacado: false, codigoUnidadFE: 'Kg' }]);
    assert.ok(!reg.creados.includes('img'));
    assert.equal(reg.innerHTML.length, 0);
    const fila = els.presentacionesBody.children[0];
    assert.equal(fila.children[0].textContent, PAYLOAD);
    assert.equal(fila.children[1].textContent, 'No');
    assert.equal(fila.children[2].textContent, 'Kg');
});

test('empacada no muestra unidad FE como relevante', () => {
    const { ctx, els } = navegador();
    ctx.renderPresentaciones([{ idPresentacion: 2, nombre: 'Caja', empacado: true, codigoUnidadFE: 'Kg' }]);
    assert.equal(els.presentacionesBody.children[0].children[2].textContent, '—');
});

test('el selector de unidad solo se muestra cuando no es empacado', () => {
    const { ctx, els } = navegador();
    els.presentacionEmpacado.checked = true;
    ctx.actualizarUnidadFeVisible();
    assert.equal(els.divUnidadFe.style.display, 'none');
    els.presentacionEmpacado.checked = false;
    ctx.actualizarUnidadFeVisible();
    assert.equal(els.divUnidadFe.style.display, 'block');
});

test('nueva presentación abre con empacado marcado por defecto', () => {
    const { ctx, els } = navegador();
    ctx.abrirPresentacion(null);
    assert.equal(els.presentacionEmpacado.checked, true);
    assert.equal(els.divUnidadFe.style.display, 'none');
});

test('no empacada sin unidad no se envía; empacada envía codigoUnidadFE null', async () => {
    const { ctx, els, reg } = navegador();
    els.presentacionNombre.value = 'Litro';
    els.presentacionEmpacado.checked = false;
    els.presentacionUnidadFe.value = '';
    await ctx.guardarPresentacion();
    assert.equal(reg.cuerpos.length, 0);
    assert.ok(els.presentacionModalError.textContent.length > 0);

    els.presentacionEmpacado.checked = true;
    els.presentacionUnidadFe.value = 'L';
    await ctx.guardarPresentacion();
    assert.equal(reg.cuerpos.length, 2); // POST y recarga de la lista
    assert.equal(reg.cuerpos[0].init.method, 'POST');
    assert.deepEqual(JSON.parse(reg.cuerpos[0].init.body), { nombre: 'Litro', empacado: true, codigoUnidadFE: null });
});

test('400 del servidor se muestra inline en el modal', async () => {
    const { ctx, els, responder } = navegador();
    els.presentacionNombre.value = 'X';
    els.presentacionEmpacado.checked = false;
    els.presentacionUnidadFe.value = 'Kg';
    responder({ status: 400, cuerpo: { success: false, message: 'Unidad no válida' } });
    await ctx.guardarPresentacion();
    assert.equal(els.presentacionModalError.textContent, 'Unidad no válida');
    assert.equal(els.presentacionModalError.style.display, 'block');
});
