/**
 * PDF-RTS-TEXTOS.JS - Título y leyenda del PDF de la venta del régimen simplificado (T-059, ADR-078)
 *
 * Refleja en el navegador las reglas que el servidor aplica al guardar (ConfiguracionFacturacionServiceImpl), solo para
 * avisar antes de enviar: el servidor es la autoridad. Todo texto se pinta con textContent (nunca como HTML).
 */
(function (root) {
    'use strict';

    const MAX_TITULO = 120;
    const MAX_LEYENDA = 300;
    const MSG_TITULO_OBLIGATORIO = 'El título del comprobante del régimen simplificado es obligatorio';
    const MSG_TITULO_LARGO = 'El título del comprobante del régimen simplificado no puede exceder 120 caracteres';
    const MSG_TITULO_SIN_FACTURA = 'El título del comprobante del régimen simplificado debe contener la palabra «Factura»';
    const MSG_LEYENDA_LARGA = 'La leyenda del comprobante del régimen simplificado no puede exceder 300 caracteres';

    function recortar(v) {
        return v === null || v === undefined ? '' : String(v).trim();
    }

    /** Minúsculas y sin tildes (NFD sin marcas combinadas): «Fáctura» y «FACTURA» valen como «factura». */
    function normalizar(v) {
        return v.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
    }

    /** La palabra completa: «Facturación», «Facturas» o «Prefactura» no cuentan. */
    function contienePalabraFactura(titulo) {
        return /(^|[^\p{L}\p{N}])factura($|[^\p{L}\p{N}])/u.test(normalizar(titulo));
    }

    /** @returns {string|null} mensaje fijo del error, o null si el título es válido */
    function validarTitulo(valor) {
        const t = recortar(valor);
        if (t === '') return MSG_TITULO_OBLIGATORIO;
        if (t.length > MAX_TITULO) return MSG_TITULO_LARGO;
        if (!contienePalabraFactura(t)) return MSG_TITULO_SIN_FACTURA;
        return null;
    }

    /** @returns {string|null} */
    function validarLeyenda(valor) {
        return recortar(valor).length > MAX_LEYENDA ? MSG_LEYENDA_LARGA : null;
    }

    /** Vista previa en texto: título y leyenda (la leyenda vacía se oculta). */
    function pintarVistaPrevia(doc, titulo, leyenda) {
        const elTitulo = doc.getElementById('pdf-rts-prev-titulo');
        const elLeyenda = doc.getElementById('pdf-rts-prev-leyenda');
        if (elTitulo) elTitulo.textContent = recortar(titulo);
        if (elLeyenda) {
            const l = recortar(leyenda);
            elLeyenda.textContent = l;
            elLeyenda.hidden = l === '';
        }
    }

    /** Aviso del título junto al campo (textContent) y validez HTML5 del campo para el submit. */
    function avisarTitulo(doc, mensaje) {
        const campo = doc.getElementById('pdfRtsTitulo');
        const aviso = doc.getElementById('pdf-rts-titulo-error');
        if (aviso) aviso.textContent = mensaje || '';
        if (campo && campo.setCustomValidity) campo.setCustomValidity(mensaje || '');
    }

    /** Relee los dos campos, valida y repinta la vista previa. */
    function actualizar(doc) {
        const titulo = doc.getElementById('pdfRtsTitulo');
        const leyenda = doc.getElementById('pdfRtsLeyenda');
        const tv = titulo ? titulo.value : '';
        const lv = leyenda ? leyenda.value : '';
        pintarVistaPrevia(doc, tv, lv);
        avisarTitulo(doc, validarTitulo(tv) || validarLeyenda(lv));
    }

    /** Engancha los campos: cada tecla actualiza la vista previa y el aviso. */
    function enlazar(doc) {
        ['pdfRtsTitulo', 'pdfRtsLeyenda'].forEach((id) => {
            const campo = doc.getElementById(id);
            if (campo) campo.addEventListener('input', () => actualizar(doc));
        });
        actualizar(doc);
    }

    root.PdfRtsTextos = {
        validarTitulo, validarLeyenda, pintarVistaPrevia, actualizar, enlazar,
        MAX_TITULO, MAX_LEYENDA
    };
})(typeof window !== 'undefined' ? window : globalThis);
