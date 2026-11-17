// T-034 criterio 5: la lista de resultados CAByS no interpola HTML sin escapar.
// Carga cabys-utils.js en un contexto vm con un DOM simulado que registra createElement e innerHTML.
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CODIGO = fs.readFileSync(path.join(raiz, 'src/main/resources/static/modules/producto/js/cabys-utils.js'), 'utf8');
const PAYLOAD = '<img src=x onerror=alert(1)>';

function crearElemento(tag, registro) {
    const el = {
        tagName: String(tag).toUpperCase(), children: [], style: {}, value: '', checked: false,
        className: '', attrs: {}, listeners: {}, disabled: false, _text: '',
        set textContent(v) { this._text = String(v); this.children = []; },
        get textContent() { return this._text + this.children.map(c => c.textContent).join(''); },
        set innerHTML(v) { registro.innerHTML.push(String(v)); },
        get innerHTML() { return ''; },
        appendChild(c) { this.children.push(c); return c; },
        replaceChildren(...n) { this.children = [...n]; this._text = ''; },
        setAttribute(k, v) { this.attrs[k] = v; },
        addEventListener(t, f) { (this.listeners[t] ||= []).push(f); }
    };
    return el;
}

function navegador(respuestaFetch) {
    const registro = { creados: [], innerHTML: [], urls: [] };
    const elementos = {};
    const ids = ['cabysBusqueda', 'btnBuscarCabys', 'cabysResultados', 'cabysListGroup', 'cabysMensaje', 'cabysAviso',
        'codigoCabys', 'descripcionCabys', 'gravado', 'porcentajeImpuesto'];
    ids.forEach(id => { elementos[id] = crearElemento('div', registro); });
    const ctx = {
        console,
        document: {
            getElementById: id => elementos[id] || null,
            createElement(tag) { registro.creados.push(String(tag).toLowerCase()); return crearElemento(tag, registro); },
            addEventListener() {}
        },
        fetch: async (url) => { registro.urls.push(url); return respuestaFetch(); },
        encodeURIComponent
    };
    vm.createContext(ctx);
    vm.runInContext(CODIGO, ctx);
    return { ctx, elementos, registro };
}

const respuesta = (status, cuerpo) => () => ({ ok: status >= 200 && status < 300, status, json: async () => cuerpo });

test('descripcion con HTML se muestra como texto y no crea elementos img', () => {
    const { ctx, elementos, registro } = navegador(respuesta(200, {}));
    ctx.mostrarResultadosCabys([{ codigo: '1234567890123', descripcion: PAYLOAD, impuesto: 13, categorias: ['<b>x</b>', 'y'] }]);
    assert.ok(!registro.creados.includes('img'));
    assert.equal(registro.innerHTML.length, 0, 'innerHTML no debe asignarse');
    const lista = elementos.cabysListGroup;
    assert.equal(lista.children.length, 1);
    assert.ok(lista.children[0].textContent.includes(PAYLOAD));
    assert.ok(lista.children[0].textContent.includes('<b>x</b> > y'));
    assert.equal(elementos.cabysResultados.style.display, 'block');
});

test('buscar: usa el endpoint nuevo y pinta los resultados', async () => {
    const { ctx, elementos, registro } = navegador(respuesta(200, {
        success: true, data: [{ codigo: '1', descripcion: PAYLOAD, impuesto: 13 }] }));
    elementos.cabysBusqueda.value = 'jugo naranja';
    await ctx.buscarCabys();
    assert.equal(registro.urls[0], '/api/articulos/cabys/buscar?q=jugo%20naranja&top=10');
    assert.ok(!registro.creados.includes('img'));
    assert.equal(registro.innerHTML.length, 0);
    assert.equal(elementos.cabysListGroup.children.length, 1);
});

test('error 502 muestra el mensaje fijo del servidor inline como texto', async () => {
    const { ctx, elementos } = navegador(respuesta(502, { success: false, message: 'Hacienda no responde ' + PAYLOAD }));
    elementos.cabysBusqueda.value = 'jugo';
    await ctx.buscarCabys();
    assert.ok(elementos.cabysMensaje.textContent.includes('Hacienda no responde'));
    assert.equal(elementos.cabysMensaje.children[0].tagName, 'DIV');
    assert.equal(elementos.btnBuscarCabys.disabled, false);
});

test('fallo de red muestra mensaje genérico sin lanzar', async () => {
    const { ctx, elementos } = navegador(() => { throw new Error('net'); });
    elementos.cabysBusqueda.value = 'jugo';
    await ctx.buscarCabys();
    assert.ok(elementos.cabysMensaje.textContent.includes('No se pudo completar'));
});

test('seleccionar llena campos y avisa del cambio de impuesto con valor anterior y nuevo', () => {
    const { ctx, elementos } = navegador(respuesta(200, {}));
    elementos.gravado.checked = true;
    elementos.porcentajeImpuesto.value = '13.00';
    ctx.seleccionarCabys({ codigo: '2132100000100', descripcion: 'Leche', impuesto: 1 });
    assert.equal(elementos.codigoCabys.value, '2132100000100');
    assert.equal(elementos.descripcionCabys.value, 'Leche');
    assert.equal(elementos.gravado.checked, true);
    assert.equal(elementos.porcentajeImpuesto.value, '1.00');
    const aviso = elementos.cabysAviso.textContent;
    assert.ok(aviso.includes('13% (gravado)') && aviso.includes('1% (gravado)'), aviso);
});

test('seleccionar con impuesto 0 desmarca gravado; mismo impuesto no avisa', () => {
    const { ctx, elementos } = navegador(respuesta(200, {}));
    elementos.gravado.checked = true;
    elementos.porcentajeImpuesto.value = '13';
    ctx.seleccionarCabys({ codigo: '1', descripcion: 'A', impuesto: 0 });
    assert.equal(elementos.gravado.checked, false);
    assert.ok(elementos.cabysAviso.textContent.includes('0% (exento)'));
    elementos.cabysAviso.replaceChildren();
    elementos.gravado.checked = true;
    elementos.porcentajeImpuesto.value = '13.00';
    ctx.seleccionarCabys({ codigo: '2', descripcion: 'B', impuesto: 13 });
    assert.equal(elementos.cabysAviso.textContent, '');
});
