// T-038 criterio 5: el pintado de errores 400 del formulario del cliente usa textContent y no interpola HTML.
// Carga cliente-errores.js en un contexto vm con un DOM simulado.
//   node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const CODIGO = fs.readFileSync(path.join(raiz, 'src/main/resources/static/modules/cliente/js/cliente-errores.js'), 'utf8');
const PAYLOAD = '<img src=x onerror=alert(1)>';

function elemento(registro) {
    const clases = new Set();
    return {
        style: {}, _text: '', clases,
        set textContent(v) { this._text = String(v); },
        get textContent() { return this._text; },
        set innerHTML(v) { registro.innerHTML.push(String(v)); },
        classList: { add: c => clases.add(c), remove: c => clases.delete(c), contains: c => clases.has(c) }
    };
}

function formulario(campos) {
    const registro = { innerHTML: [] };
    const inputs = {}, contenedores = {};
    campos.forEach(c => { inputs[c] = elemento(registro); contenedores[c] = elemento(registro); });
    const form = {
        querySelector(sel) {
            let m = /^\[name="([A-Za-z]+)"\]$/.exec(sel);
            if (m) return inputs[m[1]] || null;
            m = /^\[data-error-for="([A-Za-z]+)"\]$/.exec(sel);
            if (m) return contenedores[m[1]] || null;
            return null;
        },
        querySelectorAll(sel) {
            if (sel === '[data-error-for]') return Object.values(contenedores);
            if (sel === '.is-invalid') return Object.values(inputs).filter(i => i.clases.has('is-invalid'));
            return [];
        }
    };
    const ctx = { console };
    vm.createContext(ctx);
    vm.runInContext(CODIGO, ctx);
    return { ctx, form, inputs, contenedores, registro };
}

const CAMPOS = ['nombre', 'telefono', 'email', 'numeroIdentificacion', 'otrasSenas'];

test('cada error se pinta junto a su campo con is-invalid y el contenedor visible', () => {
    const { ctx, form, inputs, contenedores } = formulario(CAMPOS);
    const sin = ctx.pintarErroresCliente(form, [
        { campo: 'nombre', mensaje: 'El nombre debe tener entre 3 y 100 caracteres.' },
        { campo: 'telefono', mensaje: 'El teléfono no es válido.' }
    ]);
    assert.equal(sin.length, 0);
    assert.equal(contenedores.nombre.textContent, 'El nombre debe tener entre 3 y 100 caracteres.');
    assert.equal(contenedores.nombre.style.display, 'block');
    assert.ok(inputs.nombre.classList.contains('is-invalid'));
    assert.ok(inputs.telefono.classList.contains('is-invalid'));
    assert.ok(!inputs.email.classList.contains('is-invalid'));
    assert.equal(contenedores.email.textContent, '');
});

test('el mensaje con HTML se asigna como texto: innerHTML nunca se toca', () => {
    const { ctx, form, contenedores, registro } = formulario(CAMPOS);
    ctx.pintarErroresCliente(form, [{ campo: 'email', mensaje: PAYLOAD }]);
    assert.equal(contenedores.email.textContent, PAYLOAD);
    assert.equal(registro.innerHTML.length, 0);
});

test('un campo sin contenedor se devuelve para mostrarlo de otra forma; un nombre de campo raro no entra al selector', () => {
    const { ctx, form } = formulario(CAMPOS);
    const sin = ctx.pintarErroresCliente(form, [
        { campo: 'codigoActividad', mensaje: 'x' },
        { campo: 'a"]b', mensaje: 'y' },
        null
    ]);
    assert.equal(sin.length, 3);
});

test('limpiarErroresCliente borra textos y marcas; pintar de nuevo parte de cero', () => {
    const { ctx, form, inputs, contenedores } = formulario(CAMPOS);
    ctx.pintarErroresCliente(form, [{ campo: 'nombre', mensaje: 'uno' }]);
    ctx.limpiarErroresCliente(form);
    assert.equal(contenedores.nombre.textContent, '');
    assert.equal(contenedores.nombre.style.display, 'none');
    assert.ok(!inputs.nombre.classList.contains('is-invalid'));
    ctx.pintarErroresCliente(form, [{ campo: 'nombre', mensaje: 'uno' }]);
    ctx.pintarErroresCliente(form, [{ campo: 'telefono', mensaje: 'dos' }]);
    assert.equal(contenedores.nombre.textContent, '');
    assert.equal(contenedores.telefono.textContent, 'dos');
});

test('dos errores del mismo campo se concatenan; errores ausentes o formulario nulo no fallan', () => {
    const { ctx, form, contenedores } = formulario(CAMPOS);
    ctx.pintarErroresCliente(form, [{ campo: 'nombre', mensaje: 'a.' }, { campo: 'nombre', mensaje: 'b.' }]);
    assert.equal(contenedores.nombre.textContent, 'a. b.');
    assert.equal(ctx.pintarErroresCliente(form, undefined).length, 0);
    assert.equal(ctx.pintarErroresCliente(null, []).length, 0);
});
