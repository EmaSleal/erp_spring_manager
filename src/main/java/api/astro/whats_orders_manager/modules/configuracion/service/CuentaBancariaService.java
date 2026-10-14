package api.astro.whats_orders_manager.modules.configuracion.service;

import api.astro.whats_orders_manager.modules.configuracion.model.CuentaBancaria;
import api.astro.whats_orders_manager.modules.configuracion.model.Empresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.CuentaBancariaRepository;
import api.astro.whats_orders_manager.modules.contabilidad.enums.NaturalezaCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.enums.TipoCuenta;
import api.astro.whats_orders_manager.modules.contabilidad.model.CuentaContable;
import api.astro.whats_orders_manager.modules.contabilidad.repository.CuentaContableRepository;
import api.astro.whats_orders_manager.modules.contabilidad.repository.ParametroContableRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.enums.MonedaFE;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Cuentas bancarias de la empresa y su hoja contable propia (ADR-018).
 *
 * Cada cuenta bancaria tiene una hoja bajo {@value #CODIGO_BANCOS} Bancos (agrupadora), con código
 * 1.1.01.002.NNN y nombre "&lt;entidad&gt; &lt;moneda&gt;". La hoja se crea, renombra, activa/desactiva y
 * elimina junto con la cuenta bancaria, en la misma transacción.
 *
 * Al eliminar la última hoja, 1.1.01.002 sigue siendo agrupadora (no vuelve a aceptar movimientos,
 * a diferencia de CuentaContableService.eliminar): es el grupo de las cuentas bancarias y ningún
 * cobro ni pago debe volver a registrarse contra un "Bancos" genérico.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CuentaBancariaService {

    /** Agrupadora de las hojas de cuentas bancarias. */
    public static final String CODIGO_BANCOS = "1.1.01.002";

    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final CuentaContableRepository cuentaContableRepository;
    private final ParametroContableRepository parametroContableRepository;
    private final EmpresaService empresaService;

    // ==================== CONSULTAS ====================

    /**
     * Cuentas bancarias de la empresa principal.
     * @param incluirInactivas true para incluir las desactivadas (gestión en Configuración → Empresa)
     * @return Cuentas ordenadas por orden e id
     */
    public List<CuentaBancaria> listar(boolean incluirInactivas) {
        Empresa empresa = empresaService.getEmpresaPrincipal();
        List<CuentaBancaria> cuentas = incluirInactivas
            ? cuentaBancariaRepository.findByEmpresaIdEmpresaOrderByOrdenAsc(empresa.getIdEmpresa())
            : cuentaBancariaRepository.findByEmpresaIdEmpresaAndActivaTrue(empresa.getIdEmpresa());
        return cuentas;
    }

    /**
     * Cuentas bancarias activas de la empresa principal en una moneda (selectores de cobros y pagos).
     * @param moneda Código de moneda (CRC, USD, EUR); null o vacío → todas las activas
     * @return Cuentas activas
     */
    public List<CuentaBancaria> listarActivas(String moneda) {
        Empresa empresa = empresaService.getEmpresaPrincipal();
        if (moneda == null || moneda.isBlank()) {
            return cuentaBancariaRepository.findByEmpresaIdEmpresaAndActivaTrue(empresa.getIdEmpresa());
        }
        return cuentaBancariaRepository.findByEmpresaIdEmpresaAndActivaTrueAndMonedaOrderByOrdenAscIdCuentaBancariaAsc(
            empresa.getIdEmpresa(), normalizarMoneda(moneda));
    }

    /**
     * Valida la cuenta bancaria propia de un cobro o pago a proveedor (ADR-018): debe existir, estar
     * activa y estar en la moneda del pago.
     *
     * @param idCuentaBancaria Cuenta elegida (null si no se indicó)
     * @param monedaPago       Moneda del pago
     * @param requerida        Texto de la regla para el mensaje cuando falta, p. ej.
     *                         "El cobro por Transferencia requiere la cuenta bancaria de destino"
     * @return La cuenta bancaria validada
     * @throws IllegalArgumentException si falta, no existe, está inactiva o su moneda es otra
     */
    public CuentaBancaria resolverParaPago(Integer idCuentaBancaria, MonedaFE monedaPago, String requerida) {
        if (idCuentaBancaria == null) {
            throw new IllegalArgumentException(requerida);
        }
        CuentaBancaria cuenta = cuentaBancariaRepository.findById(idCuentaBancaria)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta bancaria no encontrada: " + idCuentaBancaria));
        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new IllegalArgumentException("La cuenta bancaria " + cuenta.getEntidad()
                + " está inactiva; elija una cuenta bancaria activa");
        }
        String monedaCodigo = (monedaPago != null ? monedaPago : MonedaFE.CRC).getCodigo();
        if (cuenta.getMoneda() == null || !monedaCodigo.equalsIgnoreCase(cuenta.getMoneda().trim())) {
            throw new IllegalArgumentException("La cuenta bancaria " + cuenta.getEntidad() + " está en "
                + cuenta.getMoneda() + " y el pago en " + monedaCodigo
                + "; elija una cuenta bancaria en " + monedaCodigo);
        }
        return cuenta;
    }

    // ==================== ALTA, EDICIÓN, ESTADO Y BAJA ====================

    /**
     * Crea una cuenta bancaria y, en la misma transacción, su hoja contable: siguiente código libre
     * bajo 1.1.01.002, ACTIVO/DEUDORA, nombre "&lt;entidad&gt; &lt;moneda&gt;".
     * @throws IllegalArgumentException si falta la entidad o la moneda no es válida
     * @throws IllegalStateException    si no existe 1.1.01.002 o tiene movimientos propios
     */
    @Transactional
    public CuentaBancaria crear(String entidad, String cuentaIban, String cuentaBanco, String moneda) {
        String entidadLimpia = validarEntidad(entidad);
        String monedaLimpia = normalizarMoneda(moneda);

        CuentaContable hoja = crearHoja(entidadLimpia, monedaLimpia, cuentaIban);

        CuentaBancaria cuenta = new CuentaBancaria();
        cuenta.setEmpresa(empresaService.getEmpresaPrincipal());
        cuenta.setEntidad(entidadLimpia);
        cuenta.setCuentaIban(vacioANulo(cuentaIban));
        cuenta.setCuentaBanco(vacioANulo(cuentaBanco));
        cuenta.setMoneda(monedaLimpia);
        cuenta.setActiva(true);
        cuenta.setOrden(0);
        cuenta.setCuentaContable(hoja);

        CuentaBancaria guardada = cuentaBancariaRepository.save(cuenta);
        log.info("Cuenta bancaria {} ({}) creada con hoja contable {}", entidadLimpia, monedaLimpia, hoja.getCodigo());
        return guardada;
    }

    /**
     * Actualiza una cuenta bancaria y renombra su hoja contable a "&lt;entidad&gt; &lt;moneda&gt;".
     * @throws IllegalArgumentException si no existe, falta la entidad, la moneda no es válida, o cambia
     *                                  la moneda y la hoja tiene movimientos o la cuenta tiene pagos
     */
    @Transactional
    public CuentaBancaria actualizar(Integer id, String entidad, String cuentaIban, String cuentaBanco, String moneda) {
        CuentaBancaria cuenta = buscar(id);
        String entidadLimpia = validarEntidad(entidad);
        String monedaLimpia = normalizarMoneda(moneda);
        CuentaContable hoja = hojaDe(cuenta);

        if (!monedaLimpia.equalsIgnoreCase(cuenta.getMoneda() != null ? cuenta.getMoneda().trim() : "")) {
            if (tieneMovimientos(hoja)) {
                throw new IllegalArgumentException("No se puede cambiar la moneda de la cuenta bancaria "
                    + cuenta.getEntidad() + ": su cuenta contable " + hoja.getCodigo()
                    + " tiene movimientos. Desactívela y cree una cuenta nueva en " + monedaLimpia);
            }
            if (tienePagos(cuenta)) {
                throw new IllegalArgumentException("No se puede cambiar la moneda de la cuenta bancaria "
                    + cuenta.getEntidad() + ": tiene cobros o pagos registrados. Desactívela y cree una cuenta nueva en "
                    + monedaLimpia);
            }
        }

        cuenta.setEntidad(entidadLimpia);
        cuenta.setCuentaIban(vacioANulo(cuentaIban));
        cuenta.setCuentaBanco(vacioANulo(cuentaBanco));
        cuenta.setMoneda(monedaLimpia);

        hoja.setNombre(nombreHoja(entidadLimpia, monedaLimpia));
        cuentaContableRepository.save(hoja);

        CuentaBancaria guardada = cuentaBancariaRepository.save(cuenta);
        log.info("Cuenta bancaria {} actualizada; hoja {} renombrada a '{}'", id, hoja.getCodigo(), hoja.getNombre());
        return guardada;
    }

    /**
     * Activa o desactiva una cuenta bancaria junto con su hoja contable.
     * @throws IllegalArgumentException si no existe, o se desactiva y la hoja la usa un parámetro contable
     */
    @Transactional
    public CuentaBancaria cambiarEstado(Integer id, boolean activa) {
        CuentaBancaria cuenta = buscar(id);
        CuentaContable hoja = hojaDe(cuenta);

        if (!activa) {
            List<String> claves = parametroContableRepository.findClavesByCuentaId(hoja.getIdCuenta());
            if (!claves.isEmpty()) {
                throw new IllegalArgumentException("No se puede desactivar la cuenta bancaria " + cuenta.getEntidad()
                    + ": su cuenta contable " + hoja.getCodigo() + " la usan los parámetros contables: "
                    + String.join(", ", claves));
            }
        }

        cuenta.setActiva(activa);
        hoja.setActiva(activa);
        cuentaContableRepository.save(hoja);
        CuentaBancaria guardada = cuentaBancariaRepository.save(cuenta);
        log.info("Cuenta bancaria {} {} junto con su hoja {}", cuenta.getEntidad(),
            activa ? "activada" : "desactivada", hoja.getCodigo());
        return guardada;
    }

    /**
     * Elimina una cuenta bancaria y su hoja contable. Se rechaza si algún cobro o pago a proveedor la
     * usa o si su hoja tiene movimientos: en ese caso debe desactivarse.
     * @throws IllegalArgumentException si no existe, tiene pagos o su hoja tiene movimientos
     */
    @Transactional
    public void eliminar(Integer id) {
        CuentaBancaria cuenta = buscar(id);
        CuentaContable hoja = cuenta.getCuentaContable();

        if (tienePagos(cuenta)) {
            throw new IllegalArgumentException("No se puede eliminar la cuenta bancaria " + cuenta.getEntidad()
                + ": tiene cobros o pagos a proveedor registrados. Desactívela en su lugar");
        }
        if (hoja != null && tieneMovimientos(hoja)) {
            throw new IllegalArgumentException("No se puede eliminar la cuenta bancaria " + cuenta.getEntidad()
                + ": su cuenta contable " + hoja.getCodigo() + " tiene movimientos. Desactívela en su lugar");
        }

        if (cuenta.getEmpresa() != null && cuenta.getEmpresa().getCuentasBancarias() != null) {
            cuenta.getEmpresa().getCuentasBancarias().remove(cuenta);
        }
        cuentaBancariaRepository.delete(cuenta);
        cuentaBancariaRepository.flush();

        if (hoja != null) {
            // 1.1.01.002 sigue siendo agrupadora aunque sea la última hoja (ver Javadoc de la clase)
            CuentaContable padre = hoja.getCuentaPadre();
            if (padre != null) {
                padre.getSubcuentas().remove(hoja);
                hoja.setCuentaPadre(null);
            }
            cuentaContableRepository.delete(hoja);
            log.info("Cuenta bancaria {} eliminada junto con su hoja {}", cuenta.getEntidad(), hoja.getCodigo());
        }
    }

    // ==================== PRIVADOS ====================

    private CuentaBancaria buscar(Integer id) {
        return cuentaBancariaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cuenta bancaria no encontrada: " + id));
    }

    private static CuentaContable hojaDe(CuentaBancaria cuenta) {
        if (cuenta.getCuentaContable() == null) {
            throw new IllegalStateException("La cuenta bancaria " + cuenta.getEntidad()
                + " no tiene cuenta contable (ADR-018)");
        }
        return cuenta.getCuentaContable();
    }

    /** True si la cuenta tiene cualquier detalle de asiento (en cualquier estado). */
    private boolean tieneMovimientos(CuentaContable cuenta) {
        return cuentaContableRepository.tieneMovimientos(cuenta.getIdCuenta());
    }

    private boolean tienePagos(CuentaBancaria cuenta) {
        return cuentaBancariaRepository.estaReferenciadaPorPagos(cuenta.getIdCuentaBancaria())
            || cuentaBancariaRepository.estaReferenciadaPorPagosProveedor(cuenta.getIdCuentaBancaria());
    }

    /**
     * Crea la hoja de una cuenta bancaria nueva bajo 1.1.01.002 con el siguiente código libre.
     * Si 1.1.01.002 todavía es hoja (sin subcuentas) pasa a agrupadora, siempre que no tenga movimientos.
     */
    private CuentaContable crearHoja(String entidad, String moneda, String cuentaIban) {
        CuentaContable bancos = cuentaContableRepository.findByCodigo(CODIGO_BANCOS)
            .orElseThrow(() -> new IllegalStateException(
                "No existe la cuenta " + CODIGO_BANCOS + " (Bancos); no se puede crear la cuenta contable de la cuenta bancaria"));
        if (Boolean.TRUE.equals(bancos.getAceptaMovimientos())) {
            if (tieneMovimientos(bancos)) {
                throw new IllegalStateException("La cuenta " + CODIGO_BANCOS
                    + " (Bancos) tiene movimientos propios; no puede agrupar cuentas bancarias (ADR-017)");
            }
        }

        CuentaContable hoja = new CuentaContable();
        hoja.setCodigo(siguienteCodigo(bancos));
        hoja.setNombre(nombreHoja(entidad, moneda));
        hoja.setTipo(TipoCuenta.ACTIVO);
        hoja.setNaturaleza(NaturalezaCuenta.DEUDORA);
        hoja.setNivel(bancos.getNivel() != null ? bancos.getNivel() + 1 : 5);
        hoja.setActiva(true);
        hoja.setAceptaMovimientos(true);
        hoja.setDescripcion(truncar("Cuenta bancaria " + entidad
            + (cuentaIban != null && !cuentaIban.isBlank() ? " — " + cuentaIban.trim() : "") + " (ADR-018)", 500));
        hoja.setCuentaPadre(bancos);
        // Una cuenta con subcuentas es agrupadora (ADR-017 R1); bancos está gestionada en esta
        // transacción, el cambio se guarda al confirmar
        bancos.setAceptaMovimientos(false);
        return cuentaContableRepository.save(hoja);
    }

    /** Siguiente código libre 1.1.01.002.NNN (máximo segmento existente + 1, tres dígitos). */
    private String siguienteCodigo(CuentaContable bancos) {
        String prefijo = bancos.getCodigo() + ".";
        int max = cuentaContableRepository.findByCodigoStartingWith(prefijo).stream()
            .map(CuentaContable::getCodigo)
            .map(c -> c.substring(prefijo.length()))
            .filter(resto -> resto.matches("\\d+"))
            .mapToInt(Integer::parseInt)
            .max()
            .orElse(0);
        String codigo;
        do {
            max++;
            codigo = prefijo + String.format("%03d", max);
        } while (cuentaContableRepository.existsByCodigo(codigo));
        return codigo;
    }

    static String nombreHoja(String entidad, String moneda) {
        return truncar(entidad + " " + moneda, 200);
    }

    private static String validarEntidad(String entidad) {
        if (entidad == null || entidad.isBlank()) {
            throw new IllegalArgumentException("El nombre de la entidad bancaria es obligatorio");
        }
        String limpia = entidad.trim();
        if (limpia.length() > 100) {
            throw new IllegalArgumentException("El nombre de la entidad bancaria no puede exceder 100 caracteres");
        }
        return limpia;
    }

    private static String normalizarMoneda(String moneda) {
        String codigo = moneda != null ? moneda.trim().toUpperCase(Locale.ROOT) : "";
        boolean valida = Arrays.stream(MonedaFE.values()).anyMatch(m -> m.getCodigo().equals(codigo));
        if (!valida) {
            throw new IllegalArgumentException("Moneda de la cuenta bancaria no válida: '" + moneda
                + "'. Use CRC, USD o EUR");
        }
        return codigo;
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String truncar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max);
    }
}
