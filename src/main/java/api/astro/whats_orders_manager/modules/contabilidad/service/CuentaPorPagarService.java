package api.astro.whats_orders_manager.modules.contabilidad.service;

import api.astro.whats_orders_manager.modules.contabilidad.dto.AceptacionComprobanteDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarDTO;
import api.astro.whats_orders_manager.modules.contabilidad.dto.CuentaPorPagarVistaDTO;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoAceptacionComprobante;
import api.astro.whats_orders_manager.modules.contabilidad.enums.EstadoCuentaPorPagar;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaPorPagar;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;

import java.math.BigDecimal;
import java.util.List;

public interface CuentaPorPagarService {

    String MSG_ANULAR_MOTIVO = "El motivo de anulación debe tener entre 3 y 180 caracteres";
    String MSG_ANULAR_CON_PAGOS = "La cuenta por pagar tiene pagos activos; anule los pagos antes de anular la cuenta";
    String MSG_ANULAR_YA_ANULADA = "La cuenta por pagar ya está anulada";

    String MSG_PROVEEDOR_DE_LA_OC = "El proveedor de la cuenta por pagar debe ser el de la orden de compra";

    String MSG_ACEPTACION_ESTADO = "El estado de aceptación debe ser ACEPTADO o RECHAZADO";
    String MSG_ACEPTACION_FECHA = "La fecha de aceptación es obligatoria";
    String MSG_ACEPTACION_FECHA_FUTURA = "La fecha de aceptación no puede ser futura";
    String MSG_ACEPTACION_FECHA_ANTERIOR = "La fecha de aceptación no puede ser anterior a la fecha del comprobante";
    String MSG_ACEPTACION_NUMERO = "El número del mensaje receptor es obligatorio";
    String MSG_ACEPTACION_NUMERO_LARGO = "El número del mensaje receptor no puede exceder 50 caracteres";
    String MSG_ACEPTACION_NO_APLICA = "Una cuenta por pagar del régimen simplificado no lleva aceptación del comprobante";
    String MSG_ACEPTACION_ANULADA = "No se puede registrar la aceptación de una cuenta por pagar anulada";

    CuentaPorPagar crear(CuentaPorPagarDTO dto);

    /** Como {@link #crear} y devuelve además los avisos del alta (T-053: incluye el exceso de lo facturado sobre la OC). */
    CuentaPorPagarCreada crearConAdvertencias(CuentaPorPagarDTO dto);

    CuentaPorPagar findById(Long id);

    List<CuentaPorPagar> findAll();

    List<CuentaPorPagar> findByEstado(EstadoCuentaPorPagar estado);

    List<CuentaPorPagar> findVencidas();

    int actualizarVencidas();

    void aplicarPago(CuentaPorPagar cuenta, BigDecimal monto);

    void revertirPago(CuentaPorPagar cuenta, BigDecimal monto);

    /**
     * Anula una CxP sin pagos activos (ADR-066 §3, T-052): pasa a ANULADA con usuario, fecha y motivo, revierte su
     * asiento de compra (ADR-023), recalcula el saldo del proveedor y deja registro en la auditoría. No borra nada; si
     * viene de una orden de compra conserva el enlace y no toca el estado de la orden.
     *
     * @throws IllegalArgumentException motivo fuera de 3 a 180 caracteres (400)
     * @throws IllegalStateException    ya anulada o con pagos CONFIRMADOS (409)
     * @throws java.util.NoSuchElementException la CxP no existe (404)
     */
    CuentaPorPagar anular(Long id, String motivo, Usuario usuario);

    /**
     * Registra la aceptación (o el rechazo) del comprobante del proveedor con su mensaje receptor (T-054, ADR-067 §4):
     * estado ACEPTADO o RECHAZADO, fecha del mensaje (no futura y no anterior a la fecha del comprobante) y número. Se
     * puede volver a registrar para corregirlo. Deja registro en la auditoría.
     *
     * @throws IllegalArgumentException estado, fecha o número inválidos (400)
     * @throws IllegalStateException    la CxP está anulada (409)
     * @throws java.util.NoSuchElementException la CxP no existe (404)
     */
    CuentaPorPagar registrarAceptacion(Long id, AceptacionComprobanteDTO dto, Usuario usuario);

    /**
     * Bloquea y devuelve la CxP con datos frescos. Orden de bloqueo único para toda operación que cambie su estado o
     * saldo (anulación, pagos): primero el proveedor, luego la fila de la CxP; evita carreras e interbloqueos (T-052).
     * Debe llamarse dentro de una transacción.
     */
    CuentaPorPagar bloquear(Long id);

    /**
     * Recalcula el saldo pendiente del proveedor en CRC (ADR-011): Σ saldo × tipo de cambio de sus CxP no anuladas.
     * Es el único punto que escribe {@code proveedores.saldo_pendiente} por cuentas por pagar.
     */
    void recalcularSaldoProveedor(Proveedor proveedor);

    /** Avisos del desglose del comprobante del proveedor (IVA distinto de base × tarifa); no impiden el alta. */
    List<String> advertencias(CuentaPorPagarDTO dto);

    /** DTO de una CxP, armado en una transacción de lectura (API JSON). */
    CuentaPorPagarVistaDTO detalleVista(Long id);

    /** CxP como DTO, todas o filtradas por estado y por estado de aceptación del comprobante; ambos filtros son opcionales (API JSON). */
    List<CuentaPorPagarVistaDTO> listarVista(EstadoCuentaPorPagar estado, EstadoAceptacionComprobante aceptacion);

    /** CxP vencidas como DTO (API JSON). */
    List<CuentaPorPagarVistaDTO> vencidasVista();
}
