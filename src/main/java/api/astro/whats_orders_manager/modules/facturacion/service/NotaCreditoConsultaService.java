package api.astro.whats_orders_manager.modules.facturacion.service;

import api.astro.whats_orders_manager.modules.contabilidad.model.AsientoContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.AsientoContableRepository;
import api.astro.whats_orders_manager.modules.facturacion.dto.ComprobanteNotaDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.LineaNotaCreditoDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.NotaCreditoDetalleDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.NotaCreditoListaDTO;
import api.astro.whats_orders_manager.modules.facturacion.dto.NotaCreditoPaginaDTO;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.AmbienteHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ComprobanteElectronico;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ComprobanteElectronicoRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.service.AmbienteComprobanteGuard;
import api.astro.whats_orders_manager.modules.facturacion.enums.CodigoReferenciaNota;
import api.astro.whats_orders_manager.modules.facturacion.enums.EstadoNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.Factura;
import api.astro.whats_orders_manager.modules.facturacion.model.LineaNotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.model.NotaFactura;
import api.astro.whats_orders_manager.modules.facturacion.repository.NotaFacturaRepository;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Consultas de la pantalla de notas de crédito (T-042, ADR-047): listado filtrado y paginado en la base de datos, y
 * detalle. Todo se arma como DTO dentro de la transacción, sin entidades hacia la capa web.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotaCreditoConsultaService {

    public static final int TAMANO_DEFECTO = 20;
    public static final int TAMANO_MAXIMO = 100;

    private final NotaFacturaRepository notaRepository;
    private final ComprobanteElectronicoRepository comprobanteRepository;
    private final AsientoContableRepository asientoRepository;
    private final SustitucionElegibilidad sustitucionElegibilidad;
    private final AmbienteComprobanteGuard ambienteGuard;

    /**
     * @param desde fecha de emisión mínima, inclusive
     * @param hasta fecha de emisión máxima, inclusive (hasta el final del día)
     * @param cliente texto contenido en el nombre o la identificación del cliente, sin distinguir mayúsculas
     * @param estado estado de la nota
     * @param pagina 0-based
     * @param tamano filas por página; se acota a 1..100
     */
    public NotaCreditoPaginaDTO listar(LocalDate desde, LocalDate hasta, String cliente, EstadoNotaFactura estado,
                                       int pagina, int tamano) {
        int tamanoEfectivo = tamano <= 0 ? TAMANO_DEFECTO : Math.min(tamano, TAMANO_MAXIMO);
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), tamanoEfectivo,
            Sort.by(Sort.Order.desc("fechaEmision"), Sort.Order.desc("id")));
        Page<NotaFactura> notas = notaRepository.findAll(filtros(desde, hasta, cliente, estado), pageable);

        Map<Long, ComprobanteElectronico> vigentes = new HashMap<>();
        if (!notas.isEmpty()) {
            comprobanteRepository.findVigentesByNotaIds(notas.getContent().stream().map(NotaFactura::getId).toList())
                .forEach(c -> vigentes.put(c.getNotaFactura().getId(), c));
        }
        Optional<AmbienteHacienda> activo = ambienteGuard.ambienteActivo();
        List<NotaCreditoListaDTO> filas = notas.getContent().stream()
            .map(n -> fila(n, vigentes.get(n.getId()), activo)).toList();
        return new NotaCreditoPaginaDTO(filas, notas.getNumber(), notas.getSize(), notas.getTotalElements(),
            notas.getTotalPages());
    }

    /** @return el detalle, o vacío si la nota no existe */
    public Optional<NotaCreditoDetalleDTO> detalle(Long id) {
        return notaRepository.findById(id).map(this::detalle);
    }

    private NotaCreditoDetalleDTO detalle(NotaFactura nota) {
        Factura factura = nota.getFactura();
        ComprobanteElectronico vigente = comprobanteRepository.findFirstByNotaFactura_IdAndVigenteTrue(nota.getId())
            .orElse(null);
        Optional<AmbienteHacienda> activo = ambienteGuard.ambienteActivo();
        List<ComprobanteNotaDTO> historial = comprobanteRepository.findByNotaFactura_IdOrderByIdAsc(nota.getId())
            .stream().map(c -> ComprobanteNotaDTO.de(c, activo)).toList();
        List<LineaNotaCreditoDTO> lineas = new ArrayList<>();
        for (LineaNotaFactura l : nota.getLineas()) {
            lineas.add(new LineaNotaCreditoDTO(l.getNumeroLinea(),
                l.getProducto() != null ? l.getProducto().getDescripcion() : null, l.getDescripcion(),
                l.getCodigoCabys(), l.getCantidad(), l.getUnidadMedida(), l.getPrecioUnitario(), l.getDescuento(),
                l.getTarifaIva(), l.getMontoIva(), l.getSubtotal(), l.getTotal()));
        }
        Optional<AsientoContable> asiento = asientoRepository.findFirstByNotaFactura_IdOrderByIdAsientoAsc(nota.getId());
        boolean sustituta = nota.getEstado() == EstadoNotaFactura.APLICADA
            && sustitucionElegibilidad.puedeEmitirSustituta(factura);
        String motivoSustituta = nota.getEstado() == EstadoNotaFactura.APLICADA
            ? sustitucionElegibilidad.motivoSustitutaNoDisponible(factura).orElse(null) : null;
        return new NotaCreditoDetalleDTO(nota.getId(), SustitucionElegibilidad.numeroDeNota(nota, vigente),
            nota.getEstado().name(), nota.getTipo().name(), nota.getCodigoReferencia(), nombreDe(nota),
            nota.getRazon(), nota.getFechaEmision(), nota.getFechaAplicacion(), nota.getMoneda(),
            nota.getTipoCambio(), nota.getSubtotal(), nota.getDescuento(), nota.getImpuesto(), nota.getTotal(),
            nota.getUsuarioCreador(), factura.getIdFactura(), factura.getNumeroFactura(),
            factura.getCliente() != null ? factura.getCliente().getNombre() : null, nota.getClaveReferencia(),
            nota.getFechaEmisionReferencia(), lineas, ComprobanteNotaDTO.de(vigente, activo), historial,
            asiento.map(AsientoContable::getIdAsiento).orElse(null),
            asiento.map(AsientoContable::getNumero).orElse(null), sustituta, motivoSustituta);
    }

    private NotaCreditoListaDTO fila(NotaFactura n, ComprobanteElectronico c, Optional<AmbienteHacienda> activo) {
        Factura factura = n.getFactura();
        return new NotaCreditoListaDTO(n.getId(), SustitucionElegibilidad.numeroDeNota(n, c),
            factura.getIdFactura(), factura.getNumeroFactura(),
            factura.getCliente() != null ? factura.getCliente().getNombre() : null, n.getCodigoReferencia(),
            nombreDe(n), n.getTotal(), n.getMoneda(), n.getFechaEmision(), n.getEstado().name(),
            c != null ? c.getId() : null, c != null && c.getEstado() != null ? c.getEstado().name() : null,
            c != null ? c.getFechaEnvio() : null, c != null && c.getEstado() != null && c.puedeReenviar(),
            ComprobanteNotaDTO.consultable(c), c != null && c.getAmbiente() != null ? c.getAmbiente().name() : null,
            c == null || AmbienteComprobanteGuard.coincide(c.getAmbiente(), activo),
            c != null ? AmbienteComprobanteGuard.motivo(c.getAmbiente(), activo) : null);
    }

    private static String nombreDe(NotaFactura nota) {
        return CodigoReferenciaNota.deCodigo(nota.getCodigoReferencia()).map(CodigoReferenciaNota::getNombre)
            .orElse(null);
    }

    private static Specification<NotaFactura> filtros(LocalDate desde, LocalDate hasta, String cliente,
                                                      EstadoNotaFactura estado) {
        return (root, query, cb) -> {
            // Una sola unión a factura y cliente: en la consulta de datos es también el fetch (sin N+1); en el
            // conteo es una unión simple.
            boolean conteo = query.getResultType() == Long.class || query.getResultType() == long.class;
            Join<Object, Object> factura;
            Join<Object, Object> clienteJoin;
            if (conteo) {
                factura = root.join("factura", JoinType.INNER);
                clienteJoin = factura.join("cliente", JoinType.LEFT);
            } else {
                Fetch<Object, Object> f = root.fetch("factura", JoinType.INNER);
                factura = (Join<Object, Object>) f;
                Fetch<Object, Object> c = f.fetch("cliente", JoinType.LEFT);
                clienteJoin = (Join<Object, Object>) c;
            }
            List<Predicate> p = new ArrayList<>();
            if (desde != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                p.add(cb.lessThan(root.get("fechaEmision"), hasta.plusDays(1).atStartOfDay()));
            }
            if (estado != null) {
                p.add(cb.equal(root.get("estado"), estado));
            }
            if (cliente != null && !cliente.isBlank()) {
                String like = "%" + escapar(cliente.trim().toLowerCase(Locale.ROOT)) + "%";
                p.add(cb.or(
                    cb.like(cb.lower(clienteJoin.get("nombre")), like, '\\'),
                    cb.like(cb.lower(clienteJoin.get("identificacion")), like, '\\'),
                    cb.like(cb.lower(clienteJoin.get("numeroIdentificacion")), like, '\\')));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
