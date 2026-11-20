package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.facturacion.dto.DesgloseImpuesto;
import api.astro.whats_orders_manager.modules.facturacion.dto.EmitirNotaCreditoRequest.LineaAcreditar;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.EstadoComprobante;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.ComprobanteElectronicoService;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.DatosFeProducto;
import api.astro.whats_orders_manager.modules.facturacion.electronica.util.HoraFiscal;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Emisión de notas de crédito electrónicas sobre facturas (ADR-037, T-032: nota total con los códigos 01 y 13; ADR-051,
 * T-047: nota parcial con los códigos 02 y 14, con un monto por línea con IVA incluido).
 *
 * La nota se crea EN_TRAMITE con sus líneas como foto de la factura y su comprobante tipo 03 generado y firmado en la
 * misma transacción. El envío a Hacienda va después, por el mismo flujo de la factura
 * ({@link api.astro.whats_orders_manager.modules.facturacion.electronica.service.EnvioHaciendaService#enviar(Long)}). Los efectos se aplican cuando Hacienda acepta
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
    private final AmbienteComprobanteGuard ambienteGuard;
    private final AtribucionFiscalService atribucionFiscal;
    private final NotaCreditoTopes topes;

    private static final Set<String> CODIGOS_PARCIALES = Set.of(CodigoReferenciaNota.CORRIGE_MONTO.getCodigo(),
        CodigoReferenciaNota.CORRIGE_MONTO_ERROR_MATERIAL.getCodigo());

    public static final String MENSAJE_PARCIAL_SIN_LINEAS = "La nota de crédito parcial (código 02 o 14) requiere "
        + "indicar las líneas de la factura y el monto a acreditar de cada una";
    public static final String MENSAJE_TOTAL_CON_LINEAS = "La nota de crédito total (código 01 o 13) no admite líneas; "
        + "para acreditar un monto por línea use el código 02 o 14";
    public static final String MENSAJE_LINEA_INVALIDA = "Cada línea debe indicar la línea de la factura y un monto a "
        + "acreditar mayor que 0 con dos decimales como máximo";
    public static final String MENSAJE_TOTAL_CON_PARCIALES =
        "La factura tiene notas parciales; acredite el resto con una nota parcial";

    /**
     * Valida el pedido de una nota (400 si falla): tipo CREDITO, código de la nota 9 válido en nota de crédito y
     * habilitado en esta fase (01, 02, 13 o 14), razón de 3 a 180 caracteres.
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
     * {@link #validarPedido(TipoNotaFactura, String, String)} más la coherencia entre código y modalidad (ADR-051): 02 y
     * 14 llevan líneas ({@code idLineaFactura} y {@code montoAcreditar} mayor que 0 con dos decimales, sin repetir
     * línea); 01 y 13 no.
     */
    public static CodigoReferenciaNota validarPedido(TipoNotaFactura tipo, String codigoReferencia, String razon,
                                                     List<LineaAcreditar> lineas) {
        CodigoReferenciaNota codigo = validarPedido(tipo, codigoReferencia, razon);
        boolean conLineas = lineas != null && !lineas.isEmpty();
        if (codigo.esParcial()) {
            if (!conLineas) {
                throw new NotaFacturaPedidoInvalidoException(MENSAJE_PARCIAL_SIN_LINEAS);
            }
            Set<Integer> vistas = new HashSet<>();
            for (LineaAcreditar l : lineas) {
                if (l == null || l.idLineaFactura() == null || l.montoAcreditar() == null
                    || l.montoAcreditar().signum() <= 0 || l.montoAcreditar().stripTrailingZeros().scale() > 2) {
                    throw new NotaFacturaPedidoInvalidoException(MENSAJE_LINEA_INVALIDA);
                }
                if (!vistas.add(l.idLineaFactura())) {
                    throw new NotaFacturaPedidoInvalidoException("La línea de factura " + l.idLineaFactura()
                        + " está repetida en la nota");
                }
            }
        } else if (conLineas) {
            throw new NotaFacturaPedidoInvalidoException(MENSAJE_TOTAL_CON_LINEAS);
        }
        return codigo;
    }

    /** Emite la nota de crédito total de la factura (01 o 13); ver {@link #emitirNotaCredito}. */
    @Transactional
    public NotaFactura emitirNotaCreditoTotal(Integer idFactura, TipoNotaFactura tipo, String codigoReferencia,
                                              String razon, String usuario) {
        return emitirNotaCredito(idFactura, tipo, codigoReferencia, razon, null, usuario);
    }

    /**
     * Emite la nota de crédito de la factura: total (01 o 13, sin líneas) o parcial (02 o 14, con el monto a
     * acreditar por línea, IVA incluido).
     *
     * @throws NotaFacturaPedidoInvalidoException pedido inválido (400)
     * @throws NoSuchElementException         la factura no existe (404)
     * @throws NotaFacturaConflictoException  la factura no admite la nota (409); todo se verifica antes del consecutivo
     */
    @Transactional
    public NotaFactura emitirNotaCredito(Integer idFactura, TipoNotaFactura tipo, String codigoReferencia,
                                         String razon, List<LineaAcreditar> lineasPedidas, String usuario) {
        CodigoReferenciaNota codigo = validarPedido(tipo, codigoReferencia, razon, lineasPedidas);

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
        // T-044 (P-099): el comprobante referenciado debe ser del ambiente activo; antes del consecutivo y de guardar la nota
        ambienteGuard.motivoReferencia(aceptado).ifPresent(motivo -> {
            throw new NotaFacturaConflictoException(motivo);
        });
        if (factura.estaAnulada()) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura() + " ya está anulada");
        }
        if (factura.estaAcreditadaTotalmente()) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura()
                + " ya está acreditada totalmente por notas de crédito");
        }
        if (codigo.esTotal() && notaRepository.existsByFactura_IdFacturaAndEstadoInAndCodigoReferenciaIn(idFactura,
                NotaCreditoTopes.CUENTAN, CODIGOS_PARCIALES)) {
            throw new NotaFacturaConflictoException(MENSAJE_TOTAL_CON_PARCIALES);
        }
        if (codigo.esParcial()) {
            if (notaRepository.existsByFactura_IdFacturaAndEstadoIn(idFactura, EnumSet.of(EstadoNotaFactura.EN_TRAMITE))) {
                throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura()
                    + " ya tiene una nota de crédito en trámite");
            }
        } else if (notaRepository.existsByFactura_IdFacturaAndEstadoIn(idFactura, NotaCreditoTopes.CUENTAN)) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura()
                + " ya tiene una nota de crédito en trámite o aplicada");
        }
        if (!pagoService.findPagosValidosByFacturaId(idFactura).isEmpty()) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura() + " tiene pagos "
                + "aplicados: la nota de crédito sobre una factura con pagos se habilitará con el saldo a favor "
                + "del cliente");
        }

        LocalDateTime emision = HoraFiscal.ahora(clock);
        NotaFactura nota = NotaFactura.builder()
            .factura(factura)
            .tipo(TipoNotaFactura.CREDITO)
            .codigoReferencia(codigo.getCodigo())
            .razon(razon.trim())
            .estado(EstadoNotaFactura.EN_TRAMITE)
            .fechaEmision(emision)
            // ADR-050: fecha del asiento y mes del IVA se fijan al emitir y no cambian
            .fechaContable(atribucionFiscal.fechaContable(emision.toLocalDate()))
            .periodoFiscalAtribucion(atribucionFiscal.periodoTexto(codigo, emision.toLocalDate(),
                aceptado.getFechaEmision()))
            .comprobanteReferencia(aceptado)
            .claveReferencia(aceptado.getClaveNumerica())
            .fechaEmisionReferencia(aceptado.getFechaEmision())
            .moneda(factura.getMonedaFE() != null ? factura.getMonedaFE().getCodigo() : "CRC")
            .tipoCambio(factura.getTipoCambio() != null ? factura.getTipoCambio() : BigDecimal.ONE)
            .usuarioCreador(usuario)
            .build();
        if (codigo.esParcial()) {
            fotografiarLineasParciales(factura, nota, lineasPedidas);
            exigirTopes(factura, nota, aceptado);
        } else {
            fotografiarLineas(factura, nota);
            exigirTotalDelComprobante(nota, aceptado);
        }

        try {
            nota = notaRepository.saveAndFlush(nota);
        } catch (DataIntegrityViolationException e) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura()
                + " ya tiene una nota de crédito en trámite");
        }

        ComprobanteElectronico comprobante = comprobanteService.generarYFirmarDesdeNota(nota);
        log.info("Nota de crédito {} ({}) emitida sobre la factura {} (código {}): comprobante {} en {}", nota.getId(),
            codigo.esParcial() ? "parcial" : "total", factura.getNumeroFactura(), codigo.getCodigo(),
            comprobante.getClaveNumerica(), comprobante.getEstado());
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
        List<String> sinCabys = DatosFeProducto.productosSinCabys(factura.getLineas());
        if (!sinCabys.isEmpty()) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura() + " tiene productos sin "
                + "código CAByS: " + String.join(", ", sinCabys) + ". Regístrelo en el artículo maestro antes de emitir la nota.");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;
        int numero = 1;
        for (LineaFactura linea : factura.getLineas()) {
            var producto = linea.getProducto();
            BigDecimal cantidad = linea.getCantidad() != null ? BigDecimal.valueOf(linea.getCantidad()) : BigDecimal.ONE;
            DesgloseImpuesto desglose = topes.desglosar(linea);
            BigDecimal precioSinIva = cantidad.signum() > 0
                ? desglose.base().divide(cantidad, 5, RoundingMode.HALF_UP)
                : desglose.base();
            nota.agregarLinea(LineaNotaFactura.builder()
                .lineaFactura(linea)
                .producto(producto)
                .numeroLinea(numero++)
                .descripcion(DatosFeProducto.detalle(producto))
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

    /**
     * Foto de las líneas de una nota parcial (ADR-051 §4): cada línea pedida conserva producto, CAByS, unidad,
     * descripción y cantidad de la línea original y la referencia. Base e IVA salen del monto acreditado (con IVA
     * incluido) con la tarifa de esa línea (ADR-009): base = monto / (1 + tarifa) redondeada al céntimo e impuesto =
     * monto - base, de modo que base + impuesto es exactamente el monto pedido. Precio unitario = base / cantidad con
     * 5 decimales. Los totales de la nota son la suma de sus líneas.
     */
    private void fotografiarLineasParciales(Factura factura, NotaFactura nota, List<LineaAcreditar> pedidas) {
        Map<Integer, BigDecimal> montoPorLinea = new java.util.LinkedHashMap<>();
        for (LineaAcreditar l : pedidas) {
            montoPorLinea.put(l.idLineaFactura(), l.montoAcreditar());
        }
        Set<Integer> idsFactura = new HashSet<>();
        List<LineaFactura> seleccionadas = new java.util.ArrayList<>();
        for (LineaFactura linea : factura.getLineas()) {
            idsFactura.add(linea.getIdLineaFactura());
            if (montoPorLinea.containsKey(linea.getIdLineaFactura())) {
                seleccionadas.add(linea);
            }
        }
        for (Integer id : montoPorLinea.keySet()) {
            if (!idsFactura.contains(id)) {
                throw new NotaFacturaConflictoException("La línea " + id + " no pertenece a la factura "
                    + factura.getNumeroFactura());
            }
        }
        List<String> sinCabys = DatosFeProducto.productosSinCabys(seleccionadas);
        if (!sinCabys.isEmpty()) {
            throw new NotaFacturaConflictoException("La factura " + factura.getNumeroFactura() + " tiene productos sin "
                + "código CAByS: " + String.join(", ", sinCabys) + ". Regístrelo en el artículo maestro antes de emitir la nota.");
        }
        Map<Integer, BigDecimal> acreditadoPorLinea = topes.acreditadoPorLinea(factura.getIdFactura());
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;
        int numero = 1;
        for (LineaFactura linea : seleccionadas) {
            var producto = linea.getProducto();
            BigDecimal monto = montoPorLinea.get(linea.getIdLineaFactura());
            // Tope por línea: lo acreditado (APLICADAS y EN_TRAMITE) más el monto nuevo no supera el total de la línea
            BigDecimal totalLinea = topes.totalDeLinea(linea);
            BigDecimal yaAcreditado = acreditadoPorLinea.getOrDefault(linea.getIdLineaFactura(), BigDecimal.ZERO);
            BigDecimal disponible = totalLinea.subtract(yaAcreditado);
            if (monto.compareTo(disponible) > 0) {
                throw new NotaFacturaConflictoException("El monto a acreditar de la línea " + linea.getNumeroLinea()
                    + " (" + monto.toPlainString() + ") supera el disponible de la línea ("
                    + disponible.max(BigDecimal.ZERO).toPlainString() + " de " + totalLinea.toPlainString() + ")");
            }
            BigDecimal cantidad = NotaCreditoTopes.cantidad(linea);
            DesgloseImpuesto desglose = calculadoraImpuestoService.desglosar(monto,
                calculadoraImpuestoService.resolverTarifa(producto), true);
            BigDecimal precioSinIva = cantidad.signum() > 0
                ? desglose.base().divide(cantidad, 5, RoundingMode.HALF_UP)
                : desglose.base();
            nota.agregarLinea(LineaNotaFactura.builder()
                .lineaFactura(linea)
                .producto(producto)
                .numeroLinea(numero++)
                .descripcion(DatosFeProducto.detalle(producto))
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
        nota.setSubtotal(subtotal);
        nota.setDescuento(BigDecimal.ZERO.setScale(2));
        nota.setImpuesto(impuesto);
        nota.setTotal(subtotal.add(impuesto));
    }

    /**
     * Tope por factura (ADR-051 §3): la suma de las notas APLICADAS y EN_TRAMITE más la nueva no supera el
     * TotalComprobante del comprobante ACEPTADO.
     */
    private void exigirTopes(Factura factura, NotaFactura nota, ComprobanteElectronico aceptado) {
        BigDecimal totalComprobante = NotaCreditoTopes.totalComprobante(aceptado);
        BigDecimal acreditado = topes.acreditadoDeLaFactura(factura.getIdFactura());
        if (acreditado.add(nota.getTotal()).compareTo(totalComprobante) > 0) {
            throw new NotaFacturaConflictoException("La suma de las notas de crédito de la factura "
                + factura.getNumeroFactura() + " (" + acreditado.add(nota.getTotal()).toPlainString()
                + ") supera el total del comprobante aceptado (" + totalComprobante.toPlainString()
                + "); disponible: " + totalComprobante.subtract(acreditado).max(BigDecimal.ZERO).toPlainString());
        }
    }

    /**
     * Hacienda verifica que la nota corresponda al comprobante referenciado y que las notas de crédito no superen su
     * monto (apartado d): la nota total debe sumar exactamente el TotalComprobante del comprobante ACEPTADO.
     */
    private static void exigirTotalDelComprobante(NotaFactura nota, ComprobanteElectronico aceptado) {
        BigDecimal totalComprobante = NotaCreditoTopes.totalComprobante(aceptado);
        if (totalComprobante.compareTo(nota.getTotal()) != 0) {
            throw new NotaFacturaConflictoException("El total de las líneas de la factura (" + nota.getTotal()
                + ") no coincide con el del comprobante aceptado " + aceptado.getClaveNumerica() + " ("
                + totalComprobante.stripTrailingZeros().toPlainString() + "): la nota total no se puede emitir");
        }
    }

}
