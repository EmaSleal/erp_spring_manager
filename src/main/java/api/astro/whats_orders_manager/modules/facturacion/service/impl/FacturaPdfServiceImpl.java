package api.astro.whats_orders_manager.modules.facturacion.service.impl;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.service.EmpresaService;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaPdfService;
import api.astro.whats_orders_manager.modules.facturacion.service.FacturaService;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacturaPdfServiceImpl implements FacturaPdfService {

    private final FacturaService facturaService;
    private final EmpresaService empresaService;

    @Override
    public byte[] generarPdfFactura(Integer facturaId) {
        Factura factura = facturaService.findById(facturaId)
                .orElseThrow(() -> new NoSuchElementException("Factura no encontrada: " + facturaId));

        Cliente cliente = factura.getCliente();
        List<LineaFactura> lineas = factura.getLineas();
        Empresa empresa = empresaService.getEmpresaPrincipal();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            doc.setMargins(36, 36, 36, 36);

            // Header: empresa
            doc.add(new Paragraph(safe(empresa.getNombreEmpresa()))
                    .setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            if (empresa.getRuc() != null) {
                doc.add(new Paragraph("RUC: " + empresa.getRuc())
                        .setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            }
            if (empresa.getDireccion() != null) {
                doc.add(new Paragraph(empresa.getDireccion())
                        .setFontSize(9).setTextAlignment(TextAlignment.CENTER));
            }
            if (empresa.getTelefono() != null) {
                doc.add(new Paragraph("Tel: " + empresa.getTelefono())
                        .setFontSize(9).setTextAlignment(TextAlignment.CENTER));
            }
            if (empresa.getEmail() != null) {
                doc.add(new Paragraph(empresa.getEmail())
                        .setFontSize(9).setTextAlignment(TextAlignment.CENTER));
            }

            // Logo (best-effort, never throws)
            addLogoSiExiste(doc, empresa);

            doc.add(new Paragraph("\n"));

            // Invoice metadata
            doc.add(new Paragraph("FACTURA")
                    .setBold().setFontSize(13).setTextAlignment(TextAlignment.CENTER));

            Table meta = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            addMetaRow(meta, "N° Factura:", safe(factura.getNumeroFactura()));
            addMetaRow(meta, "Serie:", safe(factura.getSerie()));
            addMetaRow(meta, "Fecha emisión:", factura.getFechaEmision() != null ? factura.getFechaEmision().toString() : "");
            addMetaRow(meta, "Fecha entrega:", factura.getFechaEntrega() != null ? factura.getFechaEntrega().toString() : "");
            addMetaRow(meta, "Condición de venta:", factura.getCondicionVentaFE() != null ? factura.getCondicionVentaFE().name() : "");
            addMetaRow(meta, "Medio de pago:", factura.getMedioPagoFE() != null ? factura.getMedioPagoFE().name() : "");
            addMetaRow(meta, "Moneda:", factura.getMonedaFE() != null ? factura.getMonedaFE().name() : "");
            // addMetaRow(meta, "Estado de pago:", factura.getEstadoPago() != null ? factura.getEstadoPago().name() : "");
            doc.add(meta);

            doc.add(new Paragraph("\n"));

            // Cliente block
            doc.add(new Paragraph("DATOS DEL CLIENTE").setBold().setFontSize(11));
            Table clienteTable = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
            addMetaRow(clienteTable, "Nombre:", cliente != null ? safe(cliente.getNombre()) : "");
            addMetaRow(clienteTable, "Identificación:", cliente != null ? safe(cliente.getIdentificacion()) : "");
            doc.add(clienteTable);

            doc.add(new Paragraph("\n"));

            // Line items table
            doc.add(new Paragraph("DETALLE").setBold().setFontSize(11));
            Table lineasTable = new Table(UnitValue.createPercentArray(new float[]{0.5f, 3f, 1f, 1.5f, 1.5f}))
                    .useAllAvailableWidth();

            // Header row
            for (String header : new String[]{"#", "Descripción", "Cantidad", "Precio Unit.", "Subtotal"}) {
                lineasTable.addHeaderCell(new Cell().add(new Paragraph(header).setBold().setFontSize(9))
                        .setBackgroundColor(ColorConstants.LIGHT_GRAY));
            }

            if (lineas != null) {
                int lineNum = 1;
                for (LineaFactura linea : lineas) {
                    String nombre = linea.getProducto() != null ? safe(linea.getProducto().getDescripcion()) : "";
                    lineasTable.addCell(cell(String.valueOf(lineNum++)));
                    lineasTable.addCell(cell(nombre));
                    lineasTable.addCell(cell(linea.getCantidad() != null ? linea.getCantidad().toString() : "0"));
                    lineasTable.addCell(cell(formatDecimal(linea.getPrecioUnitario())));
                    lineasTable.addCell(cell(formatDecimal(linea.getSubtotal())));
                }
            }
            doc.add(lineasTable);

            doc.add(new Paragraph("\n"));

            // Totals block — fallback to sum of lines if factura totals are null
            BigDecimal subtotalPdf = factura.getSubtotal();
            if (subtotalPdf == null && lineas != null) {
                subtotalPdf = lineas.stream()
                        .map(l -> l.getSubtotal() != null ? l.getSubtotal() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            BigDecimal igvPdf = factura.getImpuesto() != null ? factura.getImpuesto() : BigDecimal.ZERO;
            BigDecimal totalPdf = factura.getTotal() != null ? factura.getTotal() : (subtotalPdf != null ? subtotalPdf.add(igvPdf) : BigDecimal.ZERO);

            Table totals = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();
            addTotalRow(totals, "Subtotal:", formatDecimal(subtotalPdf));
            addTotalRow(totals, "Impuesto (IVA):", formatDecimal(igvPdf));
            addTotalRow(totals, "TOTAL:", formatDecimal(totalPdf));
            doc.add(totals);

            doc.add(new Paragraph("\n"));

            // Footer
            doc.add(new Paragraph("Documento generado el " + LocalDate.now())
                    .setFontSize(8).setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY));

        } catch (Exception e) {
            log.error("Error al generar PDF de factura {}: {}", facturaId, e.getMessage(), e);
            throw new RuntimeException("Error al generar el PDF de la factura", e);
        }

        return baos.toByteArray();
    }

    private void addLogoSiExiste(Document doc, Empresa empresa) {
        if (!empresa.tieneLogo()) {
            return;
        }
        try {
            String logoPath = Paths.get("uploads", "empresa", empresa.getLogo()).toAbsolutePath().toString();
            Image img = new Image(ImageDataFactory.create(logoPath));
            img.setMaxWidth(120).setTextAlignment(TextAlignment.CENTER);
            doc.add(img);
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo de la empresa '{}': {}", empresa.getLogo(), e.getMessage());
        }
    }

    private void addMetaRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setBold().setFontSize(9)).setBorder(null));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "").setFontSize(9)).setBorder(null));
    }

    private void addTotalRow(Table table, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(label).setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(10)).setBorder(null));
        table.addCell(new Cell().add(new Paragraph(value).setTextAlignment(TextAlignment.RIGHT).setFontSize(10)).setBorder(null));
    }

    private Cell cell(String text) {
        return new Cell().add(new Paragraph(text != null ? text : "").setFontSize(9));
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String formatDecimal(BigDecimal value) {
        return value != null ? value.toPlainString() : "0.00";
    }
}
