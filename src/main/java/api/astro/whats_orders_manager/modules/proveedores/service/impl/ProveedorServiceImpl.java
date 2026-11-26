package api.astro.whats_orders_manager.modules.proveedores.service.impl;

import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import api.astro.whats_orders_manager.modules.proveedores.dto.ProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.RegimenHacienda;
import api.astro.whats_orders_manager.modules.proveedores.exception.RegimenNoVerificableException;
import api.astro.whats_orders_manager.modules.proveedores.exception.CedulaProveedorDuplicadaException;
import api.astro.whats_orders_manager.modules.proveedores.enums.CategoriaProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.ProveedorItem;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorItemRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.service.ProveedorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * T-058 (ADR-074 §2): crear, editar la cédula y «Verificar régimen» consultan el régimen del proveedor en /fe/ae. Esa
 * consulta es una llamada HTTP lenta y nunca corre dentro de una transacción de base de datos: estos métodos no son
 * transaccionales; consultan a Hacienda primero y guardan después en una transacción corta ({@link TransactionTemplate}).
 * Si la consulta falla o no responde se guarda SIN_VERIFICAR y el proveedor se crea igual.
 */
@Slf4j
@Service
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorItemRepository proveedorItemRepository;
    private final ProductoRepository productoRepository;
    private final HaciendaApiService haciendaApiService;
    private final Clock clock;
    private final TransactionTemplate tx;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository, ProveedorItemRepository proveedorItemRepository,
                                ProductoRepository productoRepository, HaciendaApiService haciendaApiService, Clock clock,
                                PlatformTransactionManager transactionManager) {
        this.proveedorRepository = proveedorRepository;
        this.proveedorItemRepository = proveedorItemRepository;
        this.productoRepository = productoRepository;
        this.haciendaApiService = haciendaApiService;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** Régimen obtenido de /fe/ae y el momento de la consulta (null si no se obtuvo). */
    private record RegimenConsultado(RegimenHacienda regimen, LocalDateTime verificadoEn) {
        static RegimenConsultado sinVerificar() {
            return new RegimenConsultado(RegimenHacienda.SIN_VERIFICAR, null);
        }

        boolean obtenido() {
            return regimen != RegimenHacienda.SIN_VERIFICAR;
        }
    }

    /** Consulta /fe/ae sin transacción abierta; nunca lanza: cualquier fallo es SIN_VERIFICAR. */
    private RegimenConsultado consultarRegimen(String cedula) {
        try {
            RegimenHacienda regimen = RegimenHacienda.deConsulta(haciendaApiService.consultarContribuyente(cedula));
            return regimen == RegimenHacienda.SIN_VERIFICAR ? RegimenConsultado.sinVerificar()
                : new RegimenConsultado(regimen, LocalDateTime.now(clock));
        } catch (RuntimeException e) {
            log.warn("No se pudo consultar el régimen del proveedor en Hacienda: {}", e.getClass().getSimpleName());
            return RegimenConsultado.sinVerificar();
        }
    }

    private static void aplicarRegimen(Proveedor p, RegimenConsultado r) {
        p.setRegimenHacienda(r.regimen());
        p.setRegimenVerificadoEn(r.verificadoEn());
    }

    @Override
    public Proveedor crear(ProveedorDTO dto) {
        if (proveedorRepository.existsByCedula(dto.getCedula())) {
            throw new CedulaProveedorDuplicadaException(dto.getCedula());
        }
        RegimenConsultado regimen = consultarRegimen(dto.getCedula());
        return tx.execute(status -> {
            if (proveedorRepository.existsByCedula(dto.getCedula())) {
                throw new CedulaProveedorDuplicadaException(dto.getCedula());
            }
            Proveedor p = toEntity(dto);
            aplicarRegimen(p, regimen);
            Proveedor guardado = proveedorRepository.save(p);
            sincronizarItems(guardado, dto);
            log.info("Proveedor {} creado: {}", guardado.getCedula(), guardado.getNombre());
            return guardado;
        });
    }

    @Override
    public Proveedor actualizar(Long id, ProveedorDTO dto) {
        // solo un cambio de cédula vuelve a consultar el régimen (la lectura previa es de su propia transacción corta)
        String cedulaActual = findById(id).getCedula();
        boolean cedulaCambio = !cedulaActual.equals(dto.getCedula());
        RegimenConsultado regimen = cedulaCambio ? consultarRegimen(dto.getCedula()) : null;
        return tx.execute(status -> {
            Proveedor p = findById(id);
            if (!p.getCedula().equals(dto.getCedula()) && proveedorRepository.existsByCedula(dto.getCedula())) {
                throw new CedulaProveedorDuplicadaException(dto.getCedula());
            }
            boolean cambioEnTransaccion = !p.getCedula().equals(dto.getCedula());
            mapFromDto(p, dto);
            if (cambioEnTransaccion) {
                // la cédula cambió: el régimen anterior ya no es del proveedor; si no se pudo consultar queda SIN_VERIFICAR
                aplicarRegimen(p, regimen != null ? regimen : RegimenConsultado.sinVerificar());
            }
            Proveedor guardado = proveedorRepository.save(p);
            sincronizarItems(guardado, dto);
            return guardado;
        });
    }

    @Override
    public Proveedor verificarRegimen(Long id) {
        String cedula = findById(id).getCedula();
        RegimenConsultado regimen = consultarRegimen(cedula);
        if (!regimen.obtenido()) {
            throw new RegimenNoVerificableException();
        }
        return tx.execute(status -> {
            Proveedor p = findById(id);
            if (!p.getCedula().equals(cedula)) {
                // la cédula cambió mientras se consultaba: el resultado no es de este proveedor
                throw new RegimenNoVerificableException();
            }
            aplicarRegimen(p, regimen);
            return proveedorRepository.save(p);
        });
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Proveedor p = findById(id);
        p.setActivo(false);
        proveedorRepository.save(p);
        log.info("Proveedor {} desactivado", p.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proveedor> findAll() {
        return proveedorRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Proveedor findById(Long id) {
        return proveedorRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Proveedor no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proveedor> findActivos() {
        return proveedorRepository.findByActivoTrueOrderByNombreAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Proveedor> buscar(String q) {
        return proveedorRepository.buscar(q);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorItem> findItems(Long proveedorId, CategoriaProveedor tipo) {
        return proveedorItemRepository.findByProveedorIdAndTipo(proveedorId, tipo);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<Integer>> findItemIdsByCategoria(CategoriaProveedor tipo) {
        Map<Long, List<Integer>> map = new LinkedHashMap<>();
        for (ProveedorItem item : proveedorItemRepository.findByTipo(tipo)) {
            map.computeIfAbsent(item.getProveedor().getId(), k -> new ArrayList<>())
               .add(item.getItemId());
        }
        return map;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void sincronizarItems(Proveedor proveedor, ProveedorDTO dto) {
        proveedorItemRepository.deleteByProveedorId(proveedor.getId());
        CategoriaProveedor cat = dto.getCategoria();
        if (cat == null || dto.getItemIds() == null || dto.getItemIds().isEmpty()) return;
        for (Integer itemId : dto.getItemIds()) {
            String desc = productoRepository.findById(itemId)
                .map(Producto::getDescripcion)
                .orElse("Ítem " + itemId);
            proveedorItemRepository.save(ProveedorItem.builder()
                .proveedor(proveedor)
                .tipo(cat)
                .itemId(itemId)
                .descripcion(desc)
                .build());
        }
    }

    private Proveedor toEntity(ProveedorDTO dto) {
        return Proveedor.builder()
            .cedula(dto.getCedula())
            .nombre(dto.getNombre())
            .nombreComercial(dto.getNombreComercial())
            .tipoProveedor(dto.getTipoProveedor())
            .categoria(dto.getCategoria())
            .contacto(dto.getContacto())
            .telefono(dto.getTelefono())
            .email(dto.getEmail())
            .sitioWeb(dto.getSitioWeb())
            .direccion(dto.getDireccion())
            .ciudad(dto.getCiudad())
            .pais(dto.getPais() != null ? dto.getPais() : "Costa Rica")
            .moneda(dto.getMoneda())
            .diasCredito(dto.getDiasCredito())
            .limiteCredito(dto.getLimiteCredito())
            .descuentoHabitual(dto.getDescuentoHabitual())
            .banco(dto.getBanco())
            .numeroCuenta(dto.getNumeroCuenta())
            .cuentaIban(dto.getCuentaIban())
            .sinpeMovil(dto.getSinpeMovil())
            .notas(dto.getNotas())
            .build();
    }

    private void mapFromDto(Proveedor p, ProveedorDTO dto) {
        p.setCedula(dto.getCedula());
        p.setNombre(dto.getNombre());
        p.setNombreComercial(dto.getNombreComercial());
        p.setTipoProveedor(dto.getTipoProveedor());
        p.setCategoria(dto.getCategoria());
        p.setContacto(dto.getContacto());
        p.setTelefono(dto.getTelefono());
        p.setEmail(dto.getEmail());
        p.setSitioWeb(dto.getSitioWeb());
        p.setDireccion(dto.getDireccion());
        p.setCiudad(dto.getCiudad());
        if (dto.getPais() != null) p.setPais(dto.getPais());
        if (dto.getMoneda() != null) p.setMoneda(dto.getMoneda());
        p.setDiasCredito(dto.getDiasCredito());
        p.setLimiteCredito(dto.getLimiteCredito());
        p.setDescuentoHabitual(dto.getDescuentoHabitual());
        p.setBanco(dto.getBanco());
        p.setNumeroCuenta(dto.getNumeroCuenta());
        p.setCuentaIban(dto.getCuentaIban());
        p.setSinpeMovil(dto.getSinpeMovil());
        p.setNotas(dto.getNotas());
    }
}
