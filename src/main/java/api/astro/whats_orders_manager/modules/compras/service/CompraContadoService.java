package api.astro.whats_orders_manager.modules.compras.service;

import api.astro.whats_orders_manager.modules.compras.dto.CompraContadoDTO;
import api.astro.whats_orders_manager.modules.compras.dto.CompraContadoResultadoDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarLineaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.service.CuentaPorPagarService;
import api.astro.whats_orders_manager.modules.politicas.PoliticaIvaCompra;
import api.astro.whats_orders_manager.modules.proveedores.dto.PagoProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.MetodoPagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.PagoProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.service.PagoProveedorService;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Compra de contado (T-058, ADR-077 §1-2): registra la CxP del comprobante del proveedor y su pago por el total, desde la caja
 * general por defecto, en UNA transacción. No duplica lógica: la CxP la crea {@link CuentaPorPagarService#crearConAdvertencias}
 * (comprobante, política de IVA, asiento de compra y tope del RTS) y el pago {@link PagoProveedorService#registrarPago}
 * (saldo, asiento de pago). Ambos se unen a esta transacción: cualquier excepción revierte todo, incluido el adjunto.
 *
 * <p>Orden de bloqueos (T-052/T-053): proveedor FOR UPDATE primero —aquí, antes de leer su régimen—, luego la CxP nueva; el pago
 * vuelve a bloquear proveedor y CxP en ese orden. Ninguna consulta a Hacienda ocurre aquí: el régimen del proveedor ya está
 * guardado.
 */
@Service
@RequiredArgsConstructor
public class CompraContadoService {

    static final String DESCRIPCION_POR_DEFECTO = "Compra de contado";
    static final String MSG_TRATAMIENTO = "Cada línea debe indicar su tratamiento de IVA";

    private final ProveedorRepository proveedorRepository;
    private final CuentaPorPagarService cuentaPorPagarService;
    private final PagoProveedorService pagoProveedorService;
    private final PoliticaIvaCompra politicaIvaCompra;
    private final AdjuntoComprobanteService adjuntoService;

    @Transactional
    public CompraContadoResultadoDTO registrar(CompraContadoDTO dto, AdjuntoComprobante adjunto, Usuario usuario) {
        if (dto == null || dto.getProveedorId() == null) {
            throw new IllegalArgumentException("El proveedor es obligatorio");
        }
        // 1. proveedor FOR UPDATE antes que cualquier otra lectura o escritura; su régimen se lee ya con el bloqueo
        Proveedor proveedor = proveedorRepository.findByIdForUpdate(dto.getProveedorId())
            .orElseThrow(() -> new NoSuchElementException("Proveedor no encontrado: " + dto.getProveedorId()));

        // 2. la CxP, con toda su lógica (comprobante, política de IVA, asiento, tope del RTS)
        var creada = cuentaPorPagarService.crearConAdvertencias(armarCuenta(dto, proveedor));
        CuentaPorPagar cuenta = creada.cuenta();

        // 3. el pago por el total, con el método elegido (EFECTIVO = caja general, sin cuenta bancaria)
        MetodoPagoProveedor metodo = dto.getMetodoPago() != null ? dto.getMetodoPago() : MetodoPagoProveedor.EFECTIVO;
        PagoProveedor pago = pagoProveedorService.registrarPago(PagoProveedorDTO.builder()
            .cuentaPorPagarId(cuenta.getId())
            .monto(cuenta.getMonto())
            .metodoPago(metodo)
            .idCuentaBancariaOrigen(metodo.requiereCuentaBancaria() ? dto.getIdCuentaBancariaOrigen() : null)
            .referencia(dto.getReferencia())
            .fecha(dto.getFechaComprobante())
            .notas(DESCRIPCION_POR_DEFECTO)
            .build(), usuario);

        // 4. el adjunto, al final: si escribir el archivo falla se revierte todo; si la transacción se revierte después, se borra
        if (adjunto != null) {
            String ruta = adjuntoService.guardar(cuenta.getId(), adjunto);
            borrarSiNoConfirma(ruta);
            cuenta.setAdjuntoRuta(ruta);
            cuenta.setAdjuntoNombreOriginal(adjunto.nombreOriginal());
            cuenta.setAdjuntoTipo(adjunto.tipo());
        }
        return new CompraContadoResultadoDTO(cuenta.getId(), cuenta.getNumero(), pago.getId(), pago.getNumero(),
            creada.advertencias(), adjunto != null);
    }

    private CuentaPorPagarDTO armarCuenta(CompraContadoDTO dto, Proveedor proveedor) {
        return CuentaPorPagarDTO.builder()
            .proveedorId(proveedor.getId())
            .descripcion(dto.getDescripcion() == null || dto.getDescripcion().isBlank() ? DESCRIPCION_POR_DEFECTO : dto.getDescripcion())
            .moneda(dto.getMoneda())
            .numeroComprobanteProveedor(dto.getNumeroComprobanteProveedor())
            .claveComprobante(dto.getClaveComprobante())
            .fechaComprobante(dto.getFechaComprobante())
            .lineas(conTratamiento(dto, proveedor))
            .notas(dto.getNotas())
            .build();
    }

    /**
     * El validador de la CxP rechaza una línea sin tratamiento antes de que corra la política: si la política fija uno para esta
     * fecha y este proveedor (NO_ACREDITABLE) se completa aquí con lo que ella dice; si no, es un error 400. Las líneas se copian:
     * no se modifica lo que envió el cliente.
     */
    private List<CuentaPorPagarLineaDTO> conTratamiento(CompraContadoDTO dto, Proveedor proveedor) {
        if (dto.getLineas() == null) {
            return null;
        }
        List<CuentaPorPagarLineaDTO> lineas = new ArrayList<>();
        for (CuentaPorPagarLineaDTO l : dto.getLineas()) {
            var tratamiento = l.getTratamiento();
            if (tratamiento == null && dto.getFechaComprobante() != null) {
                tratamiento = politicaIvaCompra.reglaPara(dto.getFechaComprobante(), proveedor.getRegimenHacienda()).tratamientoFijo();
                if (tratamiento == null) {
                    throw new IllegalArgumentException(MSG_TRATAMIENTO);
                }
            }
            lineas.add(new CuentaPorPagarLineaDTO(l.getDescripcion(), l.getCuentaContableId(), l.getCodigoTarifa(), l.getBase(),
                l.getIva(), tratamiento, l.getTarifaLimite()));
        }
        return lineas;
    }

    private void borrarSiNoConfirma(String ruta) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    adjuntoService.eliminar(ruta);
                }
            }
        });
    }
}
