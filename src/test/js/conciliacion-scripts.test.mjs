// T-031 (auditoría 7d #13): conciliacion.html declaraba `const facturaModal` en su script inline y facturas.js
// declara `let facturaModal` en el ámbito global: al cargar facturas.js saltaba un SyntaxError («already declared»).
//
// Los scripts clásicos comparten un único ámbito léxico global, igual que vm.runInContext sobre un mismo contexto:
// se ejecutan en el orden de la página (script inline de la plantilla real, facturas.js, pagos.js).
//
// Cómo se corre (Node 18 o superior): node --test 'src/test/js/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const leer = (rel) => fs.readFileSync(path.join(raiz, rel), 'utf8');
const PLANTILLA = leer('src/main/resources/templates/modules/facturacion/pagos/conciliacion.html');
const FACTURAS_JS = leer('src/main/resources/static/modules/facturacion/js/facturas.js');
const PAGOS_JS = leer('src/main/resources/static/modules/facturacion/js/pagos.js');

/** Scripts inline `<script th:inline="javascript">` de la plantilla real, con la sintaxis de Thymeleaf ya inerte. */
function scriptsInline() {
    return [...PLANTILLA.matchAll(/<script(?:\s+th:inline="javascript")?>([\s\S]*?)<\/script>/g)].map((m) => m[1]);
}

test('conciliacion.html + facturas.js + pagos.js se cargan en el mismo ámbito global sin SyntaxError', () => {
    const inline = scriptsInline();
    assert.ok(inline.length > 0, 'la plantilla debe tener scripts inline');

    const modal = function () { this.show = () => {}; };
    const ctx = {
        console: { log() {}, error() {}, warn() {}, info() {} },
        bootstrap: { Modal: modal },
        document: {
            getElementById: () => ({}),
            querySelector: () => null,
            querySelectorAll: () => [],
            addEventListener() {},
        },
        window: { location: { search: '' } },
    };
    ctx.window.document = ctx.document;
    vm.createContext(ctx);

    for (const codigo of [...inline.slice(0, 1), FACTURAS_JS, PAGOS_JS, ...inline.slice(1)]) {
        try {
            vm.runInContext(codigo, ctx);
        } catch (e) {
            // Solo importa el SyntaxError de la redeclaración; un error de ejecución por el DOM simulado no cuenta
            assert.ok(!(e instanceof SyntaxError), `SyntaxError al cargar los scripts: ${e.message}`);
        }
    }
});
