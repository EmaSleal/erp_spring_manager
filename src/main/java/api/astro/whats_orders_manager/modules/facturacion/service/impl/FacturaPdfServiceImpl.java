package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.ConfiguracionEmpresaService;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
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

    @Value("${app.uploads.directorio:/app/uploads}")
    private String uploadsDir;

    @Value("${app.sellos.directorio:/app/sellos}")
    private String sellosDir;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public byte[] generarPdfFactura(Integer facturaId) {
        Factura factura = facturaService.findById(facturaId)
                .orElseThrow(() -> new NoSuchElementException("Factura no encontrada: " + facturaId));

        Cliente cliente = factura.getCliente();
        List<LineaFactura> lineas = factura.getLineas();
        Empresa empresa = empresaService.getEmpresaPrincipal();
        ConfiguracionEmpresa configuracion = configuracionEmpresaService.obtenerOCrearConfiguracion();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 36, 36);

            addHeader(doc, empresa, configuracion, factura);

            doc.add(separator());

            addClienteBlock(doc, cliente, factura, configuracion);

            doc.add(separator());

            addLineasTable(doc, lineas);

            doc.add(separator());

            addMontoEnLetras(doc, factura);

            doc.add(separator());

            addFooter(doc, empresa, factura);

            if (empresa.tieneTextoLegal()) {
                doc.add(separator());
                doc.add(new Paragraph(empresa.getTextoLegal())
                        .setFontSize(7)
                        .setTextAlignment(TextAlignment.JUSTIFIED));
            }

            doc.add(new Paragraph(
                    "RESOLUCIÓN NO MH-DGT-RES-0027-2024 DEL 19 DE NOVIEMBRE DE 2024 Versión 4.4  Número de página  1")
                    .setFontSize(7)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginTop(4));

        } catch (Exception e) {
            log.error("Error al generar PDF de factura {}: {}", facturaId, e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF de la factura", e);
        }

        return baos.toByteArray();
    }

    // ─── Header ───────────────────────────────────────────────────────────────

    private void addHeader(Document doc, Empresa empresa, ConfiguracionEmpresa configuracion, Factura factura) {
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
        col1.add(new Paragraph("Cédula Jurídica: " + safe(configuracion.getRuc())).setFontSize(9));
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
        col3.add(new Paragraph("Factura electrónica").setBold().setFontSize(11));
        col3.add(new Paragraph("CLAVE").setBold().setFontSize(8));
        String clave = "";
        String consecutivo = "";
        if (factura.getComprobanteElectronico() != null) {
            clave = safe(factura.getComprobanteElectronico().getClaveNumerica());
            consecutivo = safe(factura.getComprobanteElectronico().getConsecutivo());
        }
        col3.add(new Paragraph(clave).setFontSize(7));
        col3.add(new Paragraph("NÚMERO CONSECUTIVO").setBold().setFontSize(8));
        col3.add(new Paragraph(consecutivo).setFontSize(7));
        col3.add(new Paragraph("DOCUMENTO No.").setBold().setFontSize(8));
        col3.add(new Paragraph(safe(factura.getNumeroFactura())).setFontSize(9));
        col3.add(new Paragraph("ACTIVIDAD ECONOMICA").setBold().setFontSize(8));
        String actEcon = safe(configuracion.getCodigoActividad()) + " " + safe(configuracion.getDescripcionActividad());
        col3.add(new Paragraph(actEcon.trim()).setFontSize(8));
        header.addCell(col3);

        doc.add(header);
    }

    // ─── Client block ─────────────────────────────────────────────────────────

    private void addClienteBlock(Document doc, Cliente cliente, Factura factura, ConfiguracionEmpresa configuracion) {
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
                factura.getFechaEmision() != null ? factura.getFechaEmision().format(DATE_FMT) : ""));

        table.addCell(labelValueCell("Identificación:", cedula));
        table.addCell(labelValueCell("Forma Pago:",
                factura.getCondicionVentaFE() != null ? factura.getCondicionVentaFE().getDescripcion() : ""));

        table.addCell(labelValueCell("Correo:", correo));
        String vencimiento = factura.getFechaVencimiento() != null
                ? factura.getFechaVencimiento().toLocalDate().format(DATE_FMT)
                : "N/A";
        table.addCell(labelValueCell("Vencimiento:", vencimiento));

        table.addCell(labelValueCell("Dirección:", direccion));
        String plazo = factura.getPlazoCredito() != null
                ? factura.getPlazoCredito() + " días naturales"
                : "0 días naturales";
        table.addCell(labelValueCell("Plazo:", plazo));

        table.addCell(labelValueCell("Contacto:", ""));
        String vendedor = "";
        if (factura.getCreateBy() != null) {
            vendedor = usuarioService.findById(factura.getCreateBy())
                    .map(u -> safe(u.getNombre()))
                    .orElse("");
        }
        table.addCell(labelValueCell("Vendedor:", vendedor));

        table.addCell(labelValueCell("Teléfono:", telefono));
        String tipoCambio = factura.getTipoCambio() != null ? factura.getTipoCambio().toPlainString() : "";
        table.addCell(labelValueCell("Tipo Cambio:", tipoCambio));

        // last row: orden/observ spans left col, activ econ spans right
        Cell ordenCell = noB();
        ordenCell.add(new Paragraph()
                .add(new com.itextpdf.layout.element.Text("Orden Compra:").setBold())
                .add("  ")
                .add(new com.itextpdf.layout.element.Text("Observ.:").setBold())
                .add("  " + safe(factura.getDescripcion()))
                .setFontSize(9));
        table.addCell(ordenCell);
        table.addCell(labelValueCell("Activ Econ.:", safe(configuracion.getCodigoActividad())));

        doc.add(table);
    }

    // ─── Lines table ──────────────────────────────────────────────────────────

    private void addLineasTable(Document doc, List<LineaFactura> lineas) {
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

        if (lineas != null) {
            for (LineaFactura linea : lineas) {
                String um = "";
                if (linea.getProducto() != null && linea.getProducto().getPresentacion() != null) {
                    um = safe(linea.getProducto().getPresentacion().getNombre());
                }
                String codigoCabys = linea.getProducto() != null ? safe(resolveCodigoCabys(linea.getProducto())) : "";
                String codigo = linea.getProducto() != null ? safe(linea.getProducto().getCodigo()) : "";
                String detalle = linea.getProducto() != null ? safe(resolveDescripcion(linea.getProducto())) : "";
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

                table.addCell(lineCell(String.valueOf(linea.getNumeroLinea()), TextAlignment.LEFT));
                table.addCell(lineCell(codigo, TextAlignment.LEFT));
                table.addCell(lineCell(codigoCabys, TextAlignment.LEFT));
                table.addCell(lineCell(detalle, TextAlignment.LEFT));
                table.addCell(lineCell(cantidad, TextAlignment.RIGHT));
                table.addCell(lineCell(um, TextAlignment.LEFT));
                table.addCell(lineCell(precio, TextAlignment.RIGHT));
                table.addCell(lineCell(pctIva, TextAlignment.LEFT));
                table.addCell(lineCell(subtotal, TextAlignment.RIGHT));
            }
        }

        doc.add(table);
    }

    // ─── Amount in words ──────────────────────────────────────────────────────

    private void addMontoEnLetras(Document doc, Factura factura) {
        String monedaLabel = resolveMonedaLabel(factura.getMonedaFE());
        BigDecimal total = factura.getTotal() != null ? factura.getTotal() : BigDecimal.ZERO;
        String letras = MontoEnLetras.convertir(total, monedaLabel);
        doc.add(new Paragraph(letras).setFontSize(9).setBold().setMarginTop(8));
    }

    // ─── Footer ───────────────────────────────────────────────────────────────

    private void addFooter(Document doc, Empresa empresa, Factura factura) {
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
        BigDecimal subtotal = factura.getSubtotal() != null ? factura.getSubtotal() : BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        BigDecimal neto = subtotal.subtract(descuento);
        BigDecimal iva = factura.getImpuesto() != null ? factura.getImpuesto() : BigDecimal.ZERO;
        BigDecimal total = factura.getTotal() != null ? factura.getTotal() : BigDecimal.ZERO;

        String prefix = resolvePrefix(factura.getMonedaFE());
        String monedaLabel = resolveMonedaLabel(factura.getMonedaFE());

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
    private String resolveDescripcion(api.astro.whats_orders_manager.modules.producto.model.Producto p) {
        return Optional.ofNullable(p.getArticuloMaestro())
                .map(ArticuloMaestro::getDescripcion)
                .orElse(p.getDescripcion());
    }

    @SuppressWarnings("deprecation")
    private String resolveCodigoCabys(api.astro.whats_orders_manager.modules.producto.model.Producto p) {
        return Optional.ofNullable(p.getArticuloMaestro())
                .map(ArticuloMaestro::getCodigoCabys)
                .orElse(p.getCodigoCabys());
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
}
