package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.facturacion.electronica.util.DatosFeProducto;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.service.SustitucionElegibilidad;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import api.astro.whats_orders_manager.modules.facturacion.util.MontoEnLetras;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioService;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import api.astro.whats_orders_manager.modules.producto.model.ArticuloMaestro;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FacturaPdfServiceImpl implements FacturaPdfService {

    private final FacturaService facturaService;
    private final EmpresaService empresaService;
    private final ConfiguracionEmpresaService configuracionEmpresaService;
    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final UsuarioService usuarioService;
    private final NotaFacturaRepository notaFacturaRepository;
    private final ComprobanteElectronicoRepository comprobanteRepository;

    @Value("${app.uploads.directorio:/app/uploads}")
    private String uploadsDir;

    @Value("${app.sellos.directorio:/app/sellos}")
    private String sellosDir;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public byte[] generarPdfFactura(Integer facturaId) {
        Factura factura = facturaService.findById(facturaId)
                .orElseThrow(() -> new NoSuchElementException("Factura no encontrada: " + facturaId));

        ComprobanteElectronico vigente = factura.getComprobanteElectronico();
        PdfFiscalDecision decision = PdfFiscalDecision.de(
                vigente != null ? vigente.getEstado() : null,
                vigente != null ? vigente.getTipoComprobante() : null);

        try {
            return renderizar(documentoDe(factura, vigente), decision, null);
        } catch (Exception e) {
            log.error("Error al generar PDF de factura {}: {}", facturaId, e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF de la factura", e);
        }
    }

    @Override
    public byte[] generarPdfNotaCredito(Long notaId) {
        NotaFactura nota = notaFacturaRepository.findById(notaId)
                .orElseThrow(() -> new NoSuchElementException("Nota no encontrada: " + notaId));

        ComprobanteElectronico vigente = comprobanteRepository.findFirstByNotaFactura_IdAndVigenteTrue(notaId)
                .orElse(null);
        PdfFiscalDecision decision = PdfFiscalDecision.deNota(vigente != null ? vigente.getEstado() : null);

        try {
            return renderizar(documentoDe(nota, vigente, decision), decision, nota);
        } catch (Exception e) {
            log.error("Error al generar PDF de la nota {}: {}", notaId, e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF de la nota de crédito", e);
        }
    }

    /** Lo que el encabezado, el bloque del cliente, la tabla de líneas y el pie necesitan, sea factura o nota. */
    private record LineaPdf(String numero, String codigo, String codigoCabys, String detalle, String cantidad,
                            String um, String precio, String pctIva, String total) {
    }

    private record DocumentoPdf(Cliente cliente, String numero, java.time.temporal.TemporalAccessor fecha, String formaPago,
                                String vencimiento, String plazo, String vendedor, String tipoCambio,
                                String observacion, MonedaFE moneda, BigDecimal subtotal, BigDecimal impuesto,
                                BigDecimal total, ComprobanteElectronico vigente, String marcaAnulada,
                                String sustituyeA, List<LineaPdf> lineas) {
    }

    private DocumentoPdf documentoDe(Factura factura, ComprobanteElectronico vigente) {
        String vendedor = "";
        if (factura.getCreateBy() != null) {
            vendedor = usuarioService.findById(factura.getCreateBy())
                    .map(u -> safe(u.getNombre()))
                    .orElse("");
        }
        List<LineaPdf> lineas = new ArrayList<>();
        if (factura.getLineas() != null) {
            for (LineaFactura linea : factura.getLineas()) {
                // UM: la misma unidad que va en el XML (ADR-041), no el nombre de la presentación
                String um = linea.getProducto() != null ? safe(DatosFeProducto.unidadMedida(linea.getProducto())) : "";
                String codigoCabys = linea.getProducto() != null ? safe(DatosFeProducto.codigoCabys(linea.getProducto())) : "";
                String codigo = linea.getProducto() != null ? safe(linea.getProducto().getCodigo()) : "";
                String detalle = linea.getProducto() != null ? safe(DatosFeProducto.detalle(linea.getProducto())) : "";
                String cantidad = linea.getCantidad() != null
                        ? String.format("%.2f", linea.getCantidad().doubleValue())
                        : "0.00";
                String precio = linea.getPrecioUnitario() != null
                        ? String.format("%,.2f", linea.getPrecioUnitario())
                        : "0.00";
                String pctIva = linea.getProducto() != null && resolvePorcentajeImpuesto(linea.getProducto()) != null
                        ? String.format("%.2f", resolvePorcentajeImpuesto(linea.getProducto()))
                        : "0.00";
                String subtotal = linea.getSubtotal() != null
                        ? String.format("%,.2f", linea.getSubtotal())
                        : "0.00";
                lineas.add(new LineaPdf(String.valueOf(linea.getNumeroLinea()), codigo, codigoCabys, detalle, cantidad,
                        um, precio, pctIva, subtotal));
            }
        }
        String vencimiento = factura.getFechaVencimiento() != null
                ? factura.getFechaVencimiento().toLocalDate().format(DATE_FMT)
                : "N/A";
        String plazo = factura.getPlazoCredito() != null
                ? factura.getPlazoCredito() + " días naturales"
                : "0 días naturales";
        return new DocumentoPdf(factura.getCliente(), factura.getNumeroFactura(), factura.getFechaEmision(),
                factura.getCondicionVentaFE() != null ? factura.getCondicionVentaFE().getDescripcion() : "",
                vencimiento, plazo, vendedor,
                factura.getTipoCambio() != null ? factura.getTipoCambio().toPlainString() : "",
                safe(factura.getDescripcion()), factura.getMonedaFE(), factura.getSubtotal(), factura.getImpuesto(),
                factura.getTotal(), vigente, marcaAnulada(factura),
                factura.getFacturaSustituida() != null ? safe(factura.getFacturaSustituida().getNumeroFactura()) : null,
                lineas);
    }

    private DocumentoPdf documentoDe(NotaFactura nota, ComprobanteElectronico vigente, PdfFiscalDecision decision) {
        List<LineaPdf> lineas = new ArrayList<>();
        if (nota.getLineas() != null) {
            for (LineaNotaFactura linea : nota.getLineas()) {
                Producto producto = linea.getProducto();
                lineas.add(new LineaPdf(String.valueOf(linea.getNumeroLinea()),
                        producto != null ? safe(producto.getCodigo()) : "",
                        safe(linea.getCodigoCabys()), safe(linea.getDescripcion()),
                        linea.getCantidad() != null ? String.format("%.2f", linea.getCantidad().doubleValue()) : "0.00",
                        safe(linea.getUnidadMedida()),
                        linea.getPrecioUnitario() != null ? String.format("%,.2f", linea.getPrecioUnitario()) : "0.00",
                        linea.getTarifaIva() != null ? String.format("%.2f", linea.getTarifaIva()) : "0.00",
                        linea.getSubtotal() != null ? String.format("%,.2f", linea.getSubtotal()) : "0.00"));
            }
        }
        return new DocumentoPdf(nota.getFactura().getCliente(),
                // Sin consecutivo fuera de los estados fiscales: el documento se identifica como «NC-<id>»
                SustitucionElegibilidad.numeroDeNota(nota, decision.mostrarClaveYConsecutivo() ? vigente : null),
                nota.getFechaEmision(), "", "N/A", "N/A",
                safe(nota.getUsuarioCreador()),
                nota.getTipoCambio() != null ? nota.getTipoCambio().toPlainString() : "",
                safe(nota.getRazon()), MonedaFE.fromCodigo(nota.getMoneda()), nota.getSubtotal(), nota.getImpuesto(),
                nota.getTotal(), vigente, null, null, lineas);
    }

    private byte[] renderizar(DocumentoPdf d, PdfFiscalDecision decision, NotaFactura nota) throws Exception {
        Empresa empresa = empresaService.getEmpresaPrincipal();
        ConfiguracionEmpresa configuracion = configuracionEmpresaService.obtenerOCrearConfiguracion();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 36, 36);

            addHeader(doc, empresa, configuracion, d, decision);

            doc.add(separator());

            addClienteBlock(doc, d, configuracion);

            if (nota != null) {
                doc.add(separator());

                addReferenciaBlock(doc, nota);
            }

            doc.add(separator());

            addLineasTable(doc, d.lineas());

            doc.add(separator());

            addMontoEnLetras(doc, d);

            doc.add(separator());

            addFooter(doc, empresa, d);

            if (empresa.tieneTextoLegal()) {
                doc.add(separator());
                doc.add(new Paragraph(empresa.getTextoLegal())
                        .setFontSize(7)
                        .setTextAlignment(TextAlignment.JUSTIFIED));
            }

            if (decision.leyendaResolucion()) {
                doc.add(new Paragraph(
                        "RESOLUCIÓN NO MH-DGT-RES-0027-2024 DEL 19 DE NOVIEMBRE DE 2024 Versión 4.4  Número de página  1")
                        .setFontSize(7)
                        .setTextAlignment(TextAlignment.LEFT)
                        .setMarginTop(4));
            }
        }
        return baos.toByteArray();
    }

    /** Bloque «Referencia» de la nota: factura, clave, código y nombre, razón y fecha de la factura. */
    private void addReferenciaBlock(Document doc, NotaFactura nota) {
        float[] cols = {50f, 50f};
        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();
        doc.add(new Paragraph("Referencia").setBold().setFontSize(10));
        CodigoReferenciaNota codigo = CodigoReferenciaNota.deCodigo(nota.getCodigoReferencia()).orElse(null);
        table.addCell(labelValueCell("Factura:", safe(nota.getFactura().getNumeroFactura())));
        table.addCell(labelValueCell("Fecha de la factura:",
                nota.getFechaEmisionReferencia() != null ? nota.getFechaEmisionReferencia().format(DATE_FMT) : ""));
        table.addCell(labelValueCell("Clave:", safe(nota.getClaveReferencia())));
        table.addCell(labelValueCell("Código:", safe(nota.getCodigoReferencia())
                + (codigo != null ? " " + codigo.getNombre() : "")));
        table.addCell(labelValueCell("Razón:", safe(nota.getRazon())));
        table.addCell(noB());
        doc.add(table);
    }

    // ─── Header ───────────────────────────────────────────────────────────────

    private void addHeader(Document doc, Empresa empresa, ConfiguracionEmpresa configuracion, DocumentoPdf d,
                           PdfFiscalDecision decision) {
        float[] cols = {35f, 25f, 40f};
        Table header = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

        // Col 1 — company info
        Cell col1 = noB();
        if (configuracion.tieneLogoCargado()) {
            try {
                String path = Paths.get(uploadsDir, "empresa", configuracion.getLogoPath()).toString();
                Image logo = new Image(ImageDataFactory.create(path));
                logo.setMaxHeight(80);
                col1.add(logo);
            } catch (Exception e) {
                log.warn("No se pudo cargar el logo '{}': {}", configuracion.getLogoPath(), e.getMessage());
            }
        }
        col1.add(new Paragraph(safe(configuracion.getNombreEmpresa())).setBold().setFontSize(13));
        col1.add(new Paragraph(etiquetaIdentificacion(configuracion.getTipoIdentificacion()) + ": "
                + safe(configuracion.getRuc())).setFontSize(9));
        if (configuracion.getDireccion() != null) {
            col1.add(new Paragraph(configuracion.getDireccion()).setFontSize(9));
        }
        if (configuracion.getTelefono() != null) {
            col1.add(new Paragraph("Tel: " + configuracion.getTelefono()).setFontSize(9));
        }
        if (configuracion.getEmail() != null) {
            col1.add(new Paragraph(configuracion.getEmail()).setFontSize(9));
        }
        header.addCell(col1);

        // Col 2 — sello
        Cell col2 = noB().setTextAlignment(TextAlignment.CENTER);
        if (empresa.tieneSello()) {
            try {
                String path = Paths.get(sellosDir, empresa.getSelloTipoEmpresa()).toString();
                Image sello = new Image(ImageDataFactory.create(path));
                sello.setMaxHeight(90);
                col2.add(sello);
            } catch (Exception e) {
                log.warn("No se pudo cargar el sello '{}': {}", empresa.getSelloTipoEmpresa(), e.getMessage());
            }
        }
        header.addCell(col2);

        // Col 3 — invoice data
        Cell col3 = noB();
        col3.add(new Paragraph(decision.titulo()).setBold().setFontSize(11));
        if (decision.enTramite()) {
            col3.add(new Paragraph(PdfFiscalDecision.LEYENDA_EN_TRAMITE).setBold().setFontSize(8));
        }
        if (d.marcaAnulada() != null) {
            col3.add(new Paragraph(d.marcaAnulada()).setBold().setFontSize(9).setFontColor(ColorConstants.RED));
        }
        if (d.sustituyeA() != null) {
            col3.add(new Paragraph("Sustituye a " + d.sustituyeA())
                    .setFontSize(8));
        }
        if (decision.mostrarClaveYConsecutivo()) {
            ComprobanteElectronico vigente = d.vigente();
            col3.add(new Paragraph("CLAVE").setBold().setFontSize(8));
            col3.add(new Paragraph(vigente != null ? safe(vigente.getClaveNumerica()) : "").setFontSize(7));
            col3.add(new Paragraph("NÚMERO CONSECUTIVO").setBold().setFontSize(8));
            col3.add(new Paragraph(vigente != null ? safe(vigente.getConsecutivo()) : "").setFontSize(7));
        }
        col3.add(new Paragraph("DOCUMENTO No.").setBold().setFontSize(8));
        col3.add(new Paragraph(safe(d.numero())).setFontSize(9));
        col3.add(new Paragraph("ACTIVIDAD ECONOMICA").setBold().setFontSize(8));
        String actEcon = safe(configuracion.getCodigoActividad()) + " " + safe(configuracion.getDescripcionActividad());
        col3.add(new Paragraph(actEcon.trim()).setFontSize(8));
        header.addCell(col3);

        doc.add(header);
    }

    /** «ANULADA por la nota <número>» si la factura está anulada y tiene una nota de crédito aplicada; si no, null. */
    private String marcaAnulada(Factura factura) {
        if (!Boolean.TRUE.equals(factura.getAnulada()) || factura.getIdFactura() == null) {
            return null;
        }
        List<NotaFactura> notas = notaFacturaRepository.findByFactura_IdFacturaOrderByIdAsc(factura.getIdFactura());
        for (NotaFactura nota : notas) {
            if (nota.getTipo() == TipoNotaFactura.CREDITO && nota.getEstado() == EstadoNotaFactura.APLICADA) {
                ComprobanteElectronico cNota =
                        comprobanteRepository.findFirstByNotaFactura_IdAndVigenteTrue(nota.getId()).orElse(null);
                return "ANULADA por la nota " + SustitucionElegibilidad.numeroDeNota(nota, cNota);
            }
        }
        return null;
    }

    // ─── Client block ─────────────────────────────────────────────────────────

    private void addClienteBlock(Document doc, DocumentoPdf d, ConfiguracionEmpresa configuracion) {
        Cliente cliente = d.cliente();
        float[] cols = {50f, 50f};
        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

        String nombre = cliente != null ? safe(cliente.getNombre()) : "";
        String idCliente = cliente != null ? String.valueOf(cliente.getIdCliente()) : "";
        String cedula = cliente != null ? safe(cliente.getIdentificacion()) : "";
        String correo = cliente != null ? safe(cliente.getEmail()) : "";
        String direccion = cliente != null ? safe(cliente.getDireccion()) : "";
        String telefono = cliente != null ? safe(cliente.getTelefono()) : "";

        // left column
        table.addCell(labelValueCell("Cliente:", "C" + idCliente + "  " + nombre));
        // right — fecha
        table.addCell(labelValueCell("Fecha:",
                d.fecha() != null ? DATE_FMT.format(d.fecha()) : ""));

        table.addCell(labelValueCell("Identificación:", cedula));
        table.addCell(labelValueCell("Forma Pago:",
                d.formaPago()));

        table.addCell(labelValueCell("Correo:", correo));
        table.addCell(labelValueCell("Vencimiento:", d.vencimiento()));

        table.addCell(labelValueCell("Dirección:", direccion));
        table.addCell(labelValueCell("Plazo:", d.plazo()));

        table.addCell(labelValueCell("Contacto:", ""));
        table.addCell(labelValueCell("Vendedor:", d.vendedor()));

        table.addCell(labelValueCell("Teléfono:", telefono));
        table.addCell(labelValueCell("Tipo Cambio:", d.tipoCambio()));

        // last row: orden/observ spans left col, activ econ spans right
        Cell ordenCell = noB();
        ordenCell.add(new Paragraph()
                .add(new com.itextpdf.layout.element.Text("Orden Compra:").setBold())
                .add("  ")
                .add(new com.itextpdf.layout.element.Text("Observ.:").setBold())
                .add("  " + d.observacion())
                .setFontSize(9));
        table.addCell(ordenCell);
        table.addCell(labelValueCell("Activ Econ.:", safe(configuracion.getCodigoActividad())));

        doc.add(table);
    }

    // ─── Lines table ──────────────────────────────────────────────────────────

    private void addLineasTable(Document doc, List<LineaPdf> lineas) {
        float[] cols = {0.4f, 0.8f, 1.6f, 3.5f, 0.8f, 0.7f, 1.2f, 0.7f, 1.2f};
        Table table = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

        String[] headers = {"Lin", "Código", "Cod Cabys", "Detalle", "Cantidad", "UM", "Precio", "%IVA", "Total"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setBold().setFontSize(8))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setBorderBottom(new SolidBorder(0.5f))
                    .setBorderTop(Border.NO_BORDER)
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER));
        }

        for (LineaPdf l : lineas) {
            table.addCell(lineCell(l.numero(), TextAlignment.LEFT));
            table.addCell(lineCell(l.codigo(), TextAlignment.LEFT));
            table.addCell(lineCell(l.codigoCabys(), TextAlignment.LEFT));
            table.addCell(lineCell(l.detalle(), TextAlignment.LEFT));
            table.addCell(lineCell(l.cantidad(), TextAlignment.RIGHT));
            table.addCell(lineCell(l.um(), TextAlignment.LEFT));
            table.addCell(lineCell(l.precio(), TextAlignment.RIGHT));
            table.addCell(lineCell(l.pctIva(), TextAlignment.LEFT));
            table.addCell(lineCell(l.total(), TextAlignment.RIGHT));
        }

        doc.add(table);
    }

    // ─── Amount in words ──────────────────────────────────────────────────────

    private void addMontoEnLetras(Document doc, DocumentoPdf d) {
        String monedaLabel = resolveMonedaLabel(d.moneda());
        BigDecimal total = d.total() != null ? d.total() : BigDecimal.ZERO;
        String letras = MontoEnLetras.convertir(total, monedaLabel);
        doc.add(new Paragraph(letras).setFontSize(9).setBold().setMarginTop(8));
    }

    // ─── Footer ───────────────────────────────────────────────────────────────

    private void addFooter(Document doc, Empresa empresa, DocumentoPdf d) {
        float[] cols = {60f, 40f};
        Table footer = new Table(UnitValue.createPercentArray(cols)).useAllAvailableWidth();

        // Left — bank accounts
        Cell leftCell = noB();
        leftCell.add(new Paragraph("Por favor depositar en las siguientes cuentas bancarias")
                .setBold().setFontSize(8));

        List<CuentaBancaria> cuentas = cuentaBancariaRepository
                .findByEmpresaIdEmpresaAndActivaTrue(empresa.getIdEmpresa());

        if (!cuentas.isEmpty()) {
            float[] bankCols = {1f, 2f, 1.5f};
            Table bankTable = new Table(UnitValue.createPercentArray(bankCols)).useAllAvailableWidth();
            bankTable.addHeaderCell(hdrCell("Entidad"));
            bankTable.addHeaderCell(hdrCell("Cuenta IBAN"));
            bankTable.addHeaderCell(hdrCell("Cuenta Banco"));
            for (CuentaBancaria c : cuentas) {
                bankTable.addCell(bankCell(safe(c.getEntidad())));
                bankTable.addCell(bankCell(safe(c.getCuentaIban())));
                bankTable.addCell(bankCell(safe(c.getCuentaBanco())));
            }
            leftCell.add(bankTable);
        }
        footer.addCell(leftCell);

        // Right — totals
        Cell rightCell = noB();
        BigDecimal subtotal = d.subtotal() != null ? d.subtotal() : BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        BigDecimal neto = subtotal.subtract(descuento);
        BigDecimal iva = d.impuesto() != null ? d.impuesto() : BigDecimal.ZERO;
        BigDecimal total = d.total() != null ? d.total() : BigDecimal.ZERO;

        String prefix = resolvePrefix(d.moneda());
        String monedaLabel = resolveMonedaLabel(d.moneda());

        float[] totCols = {1f, 1f};
        Table totTable = new Table(UnitValue.createPercentArray(totCols)).useAllAvailableWidth();
        addTotalRow(totTable, "Sub Total", prefix + String.format("%,.2f", subtotal), false);
        addTotalRow(totTable, "Descuento", prefix + String.format("%,.2f", descuento), false);
        addTotalRow(totTable, "Neto", prefix + String.format("%,.2f", neto), false);
        addTotalRow(totTable, "IVA:", prefix + String.format("%,.2f", iva), false);
        addTotalRow(totTable, "Total", prefix + String.format("%,.2f", total), true);
        addTotalRow(totTable, "Moneda:", monedaLabel, false);
        rightCell.add(totTable);
        footer.addCell(rightCell);

        doc.add(footer);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private LineSeparator separator() {
        return new LineSeparator(new SolidLine(0.5f)).setMarginTop(4).setMarginBottom(4);
    }

    private Cell noB() {
        return new Cell().setBorder(Border.NO_BORDER);
    }

    private Cell labelValueCell(String label, String value) {
        Cell cell = noB();
        cell.add(new Paragraph()
                .add(new com.itextpdf.layout.element.Text(label).setBold())
                .add("  " + safe(value))
                .setFontSize(9));
        return cell;
    }

    private Cell lineCell(String text, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(safe(text)).setFontSize(9).setTextAlignment(alignment))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(0.3f));
    }

    private Cell hdrCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(8))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBorderBottom(new SolidBorder(0.5f))
                .setBorderTop(Border.NO_BORDER)
                .setBorderLeft(Border.NO_BORDER)
                .setBorderRight(Border.NO_BORDER);
    }

    private Cell bankCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(8))
                .setBorder(Border.NO_BORDER);
    }

    private void addTotalRow(Table table, String label, String value, boolean bold) {
        Paragraph labelP = new Paragraph(label).setFontSize(9).setTextAlignment(TextAlignment.RIGHT);
        Paragraph valueP = new Paragraph(value).setFontSize(9).setTextAlignment(TextAlignment.RIGHT);
        if (bold) {
            labelP.setBold();
            valueP.setBold();
        }
        table.addCell(new Cell().add(labelP).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(valueP).setBorder(Border.NO_BORDER));
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    @SuppressWarnings("deprecation")
    private java.math.BigDecimal resolvePorcentajeImpuesto(api.astro.whats_orders_manager.modules.producto.model.Producto p) {
        return Optional.ofNullable(p.getArticuloMaestro())
                .map(ArticuloMaestro::getPorcentajeImpuesto)
                .orElse(p.getPorcentajeImpuesto());
    }

    private String resolveMonedaLabel(MonedaFE moneda) {
        if (moneda == null) return "Colones";
        return switch (moneda) {
            case USD -> "Dólares";
            case EUR -> "Euros";
            default -> "Colones";
        };
    }

    private String resolvePrefix(MonedaFE moneda) {
        if (moneda == null) return "¢";
        return switch (moneda) {
            case USD -> "$";
            case EUR -> "€";
            default -> "¢";
        };
    }

    /**
     * ADR-021: la etiqueta de la identificación del emisor sale de su tipo (Cédula Física, Cédula Jurídica,
     * DIMEX, NITE); sin tipo configurado, "Identificación".
     */
    static String etiquetaIdentificacion(TipoIdentificacion tipo) {
        return tipo != null ? tipo.getDescripcion() : "Identificación";
    }
}
