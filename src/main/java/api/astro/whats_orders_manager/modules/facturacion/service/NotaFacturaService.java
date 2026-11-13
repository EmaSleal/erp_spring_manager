package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.DesgloseImpuesto;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.DatosFeProducto;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.XmlValidator;
import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.enums.TipoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaConflictoException;
import api.astro.whats_orders_manager.modules.facturacion.exception.NotaFacturaPedidoInvalidoException;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.FacturaRepository;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Emisión de notas de crédito electrónicas sobre facturas (ADR-037, T-032: nota total con los códigos 01 y 13).
 *
 * La nota se crea EN_TRAMITE con sus líneas como foto de la factura y su comprobante tipo 03 generado y firmado en la
 * misma transacción. El envío a Hacienda va después, por el mismo flujo de la factura
 * ({@link ComprobanteElectronicoService#procesarFacturaAsync(Long)}). Los efectos se aplican cuando Hacienda acepta
 * el comprobante ({@link NotaFacturaEfectos}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotaFacturaService {

    /** Longitud de InformacionReferencia/Razon (apartado d del Anexo v4.4). */
    public static final int RAZON_MIN = 3;
    public static final int RAZON_MAX = 180;

    private final FacturaRepository facturaRepository;
    private final NotaFacturaRepository notaRepository;
    private final ComprobanteElectronicoRepository comprobanteRepository;
    private final ComprobanteElectronicoService comprobanteService;
    private final PagoService pagoService;
    private final CalculadoraImpuestoService calculadoraImpuestoService;
    private final Clock clock;

    /**
     * Valida el pedido de una nota (400 si falla): tipo CREDITO, código de la nota 9 válido en nota de crédito y
     * habilitado en esta fase (01 o 13), razón de 3 a 180 caracteres.
     *
     * @throws NotaFacturaPedidoInvalidoException con el mensaje para el cliente
     */
    public static CodigoReferenciaNota validarPedido(TipoNotaFactura tipo, String codigoReferencia, String razon) {
        if (tipo == TipoNotaFactura.DEBITO) {
            throw new NotaFacturaPedidoInvalidoException("La nota de débito no está disponible");
        }
        CodigoReferenciaNota codigo = CodigoReferenciaNota.deCodigo(codigoReferencia == null ? null : codigoReferencia.trim())
            .orElseThrow(() -> new NotaFacturaPedidoInvalidoException(
                "Código de referencia inválido: " + codigoReferencia + " (nota 9 del Anexo v4.4)"));
        if (!codigo.isValidoEnNotaCredito()) {
            throw new NotaFacturaPedidoInvalidoException("El código de referencia " + codigo.getCodigo() + " ("
                + codigo.getNombre() + ") no se usa en una nota de crédito");
        }
        if (!codigo.isHabilitadoEnT032()) {
            throw new NotaFacturaPedidoInvalidoException("El código de referencia " + codigo.getCodigo() + " ("
                + codigo.getNombre() + ") estará disponible en una fase posterior");
        }
        String r = razon == null ? "" : razon.trim();
        if (r.length() < RAZON_MIN || r.length() > RAZON_MAX) {
            throw new NotaFacturaPedidoInvalidoException("La razón debe tener entre " + RAZON_MIN + " y " + RAZON_MAX
                + " caracteres");
        }
        return codigo;
    }

    /**
     * Emite la nota de crédito total de la factura.
     *
     * @throws NotaFacturaPedidoInvalidoException pedido inválido (400)
     * @throws NoSuchElementException         la factura no existe (404)
     * @throws NotaFacturaConflictoException  la factura no admite la nota (409)
     */
    @Transactional
    public NotaFactura emitirNotaCreditoTotal(Integer idFactura, TipoNotaFactura tipo, String codigoReferencia,
                                              String razon, String usuario) {
        CodigoReferenciaNota codigo = validarPedido(tipo, codigoReferencia, razon);

        // FOR UPDATE: dos emisiones concurrentes sobre la misma factura se serializan aquí; la segunda ve la nota de
        // la primera. uk_nota_en_tramite_por_factura (V39) lo garantiza además en la BD.
        Factura factura = facturaRepository.findByIdParaActualizar(idFactura)
            .orElseThrow(() -> new NoSuchElementException("La factura no existe"));

        ComprobanteElectronico aceptado = comprobanteRepository
            .findFirstByFactura_IdFacturaAndVigenteTrue(idFactura)
            .filter(c -> c.getEstado() == EstadoComprobante.ACEPTADO)
            .orElseThrow(() -> new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura()
                + " no tiene un comprobante electrónico ACEPTADO por Hacienda: la nota de crédito solo corrige "
                + "comprobantes aceptados. Una factura sin comprobante aceptado se corrige editándola o anulándola "
                + "(ADR-030)."));
        if (factura.estaAnulada()) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura() + " ya está anulada");
        }
        if (notaRepository.existsByFactura_IdFacturaAndEstadoIn(idFactura,
                EnumSet.of(EstadoNotaFactura.EN_TRAMITE, EstadoNotaFactura.APLICADA))) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura()
                + " ya tiene una nota de crédito en trámite o aplicada");
        }
        if (!pagoService.findPagosValidosByFacturaId(idFactura).isEmpty()) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura() + " tiene pagos "
                + "aplicados: la nota de crédito sobre una factura con pagos se habilitará con la nota parcial y el "
                + "saldo a favor");
        }

        NotaFactura nota = NotaFactura.builder()
            .factura(factura)
            .tipo(TipoNotaFactura.CREDITO)
            .codigoReferencia(codigo.getCodigo())
            .razon(razon.trim())
            .estado(EstadoNotaFactura.EN_TRAMITE)
            .fechaEmision(LocalDateTime.now(clock))
            .comprobanteReferencia(aceptado)
            .claveReferencia(aceptado.getClaveNumerica())
            .fechaEmisionReferencia(aceptado.getFechaEmision())
            .moneda(factura.getMonedaFE() != null ? factura.getMonedaFE().getCodigo() : "CRC")
            .tipoCambio(factura.getTipoCambio() != null ? factura.getTipoCambio() : BigDecimal.ONE)
            .usuarioCreador(usuario)
            .build();
        fotografiarLineas(factura, nota);
        exigirTotalDelComprobante(nota, aceptado);

        try {
            nota = notaRepository.saveAndFlush(nota);
        } catch (DataIntegrityViolationException e) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura()
                + " ya tiene una nota de crédito en trámite");
        }

        ComprobanteElectronico comprobante = comprobanteService.generarYFirmarDesdeNota(nota);
        log.info("Nota de crédito {} emitida sobre la factura {} (código {}): comprobante {} en {}", nota.getId(),
            factura.getNumeroFactura(), codigo.getCodigo(), comprobante.getClaveNumerica(), comprobante.getEstado());
        return nota;
    }

    /** La nota con su comprobante vigente (o vacío si la nota no existe). */
    @Transactional(readOnly = true)
    public Optional<NotaConComprobante> obtener(Long idNota) {
        return notaRepository.findById(idNota).map(nota -> new NotaConComprobante(nota,
            comprobanteRepository.findFirstByNotaFactura_IdAndVigenteTrue(nota.getId()).orElse(null)));
    }

    /** Nota y su comprobante vigente (null si todavía no tiene). */
    public record NotaConComprobante(NotaFactura nota, ComprobanteElectronico comprobante) {
    }

    /**
     * Copia cada línea de la factura a la nota (criterio 1b): producto, descripción, CAByS y unidad resueltos como en
     * el XML de la factura, y el desglose de IVA con el mismo componente (ADR-009). Totales = suma de las líneas.
     */
    private void fotografiarLineas(Factura factura, NotaFactura nota) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;
        int numero = 1;
        for (LineaFactura linea : factura.getLineas()) {
            var producto = linea.getProducto();
            BigDecimal cantidad = linea.getCantidad() != null ? BigDecimal.valueOf(linea.getCantidad()) : BigDecimal.ONE;
            DesgloseImpuesto desglose = desglosar(linea, cantidad);
            BigDecimal precioSinIva = cantidad.signum() > 0
                ? desglose.base().divide(cantidad, 5, RoundingMode.HALF_UP)
                : desglose.base();
            nota.agregarLinea(LineaNotaFactura.builder()
                .lineaFactura(linea)
                .producto(producto)
                .numeroLinea(numero++)
                .descripcion(truncar(DatosFeProducto.descripcion(producto), 200))
                .codigoCabys(DatosFeProducto.codigoCabys(producto))
                .unidadMedida(DatosFeProducto.unidadMedida(producto))
                .cantidad(cantidad)
                .precioUnitario(precioSinIva)
                .descuento(BigDecimal.ZERO.setScale(2))
                .tarifaIva(desglose.tarifa() != null ? desglose.tarifa() : BigDecimal.ZERO)
                .montoIva(desglose.impuesto())
                .subtotal(desglose.base())
                .total(desglose.total())
                .build());
            subtotal = subtotal.add(desglose.base());
            impuesto = impuesto.add(desglose.impuesto());
        }
        if (nota.getLineas().isEmpty()) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura() + " no tiene líneas");
        }
        nota.setSubtotal(subtotal);
        nota.setDescuento(BigDecimal.ZERO.setScale(2));
        nota.setImpuesto(impuesto);
        nota.setTotal(subtotal.add(impuesto));
    }

    /** Mismo importe de línea que el XML de la factura (XmlGeneratorServiceImpl). */
    private DesgloseImpuesto desglosar(LineaFactura linea, BigDecimal cantidad) {
        BigDecimal importe = linea.getSubtotal();
        if (importe == null) {
            BigDecimal precio = linea.getPrecioUnitario() != null ? linea.getPrecioUnitario() : BigDecimal.ZERO;
            importe = precio.multiply(cantidad);
        }
        return calculadoraImpuestoService.desglosarLinea(linea.getProducto(), importe);
    }

    /**
     * Hacienda verifica que la nota corresponda al comprobante referenciado y que las notas de crédito no superen su
     * monto (apartado d): la nota total debe sumar exactamente el TotalComprobante del comprobante ACEPTADO.
     */
    private static void exigirTotalDelComprobante(NotaFactura nota, ComprobanteElectronico aceptado) {
        String totalXml = aceptado.getXmlComprobante() != null
            ? XmlValidator.extraerValorNodo(aceptado.getXmlComprobante(), "TotalComprobante") : null;
        if (totalXml == null || totalXml.isBlank()) {
            throw new NotaFacturaConflictoException("El comprobante aceptado " + aceptado.getClaveNumerica()
                + " no tiene TotalComprobante en su XML: no se puede verificar el monto de la nota");
        }
        BigDecimal totalComprobante = new BigDecimal(totalXml.trim());
        if (totalComprobante.compareTo(nota.getTotal()) != 0) {
            throw new NotaFacturaConflictoException("El total de las líneas de la factura (" + nota.getTotal()
                + ") no coincide con el del comprobante aceptado " + aceptado.getClaveNumerica() + " ("
                + totalComprobante.stripTrailingZeros().toPlainString() + "): la nota total no se puede emitir");
        }
    }

    private static String truncar(String texto, int max) {
        if (texto == null) return "";
        return texto.length() > max ? texto.substring(0, max) : texto;
    }
}
