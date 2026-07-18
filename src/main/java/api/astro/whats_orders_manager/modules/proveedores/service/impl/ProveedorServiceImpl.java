package api.astro.whats_orders_manager.modules.proveedores.service.impl;

import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.proveedores.dto.ProveedorDTO;
import api.astro.whats_orders_manager.modules.proveedores.enums.CategoriaProveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.Proveedor;
import api.astro.whats_orders_manager.modules.proveedores.model.ProveedorItem;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorItemRepository;
import api.astro.whats_orders_manager.modules.proveedores.repository.ProveedorRepository;
import api.astro.whats_orders_manager.modules.proveedores.service.ProveedorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorItemRepository proveedorItemRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    public Proveedor crear(ProveedorDTO dto) {
        if (proveedorRepository.existsByCedula(dto.getCedula())) {
            throw new IllegalArgumentException("Ya existe un proveedor con cédula: " + dto.getCedula());
        }
        Proveedor p = toEntity(dto);
        Proveedor guardado = proveedorRepository.save(p);
        sincronizarItems(guardado, dto);
        log.info("Proveedor {} creado: {}", guardado.getCedula(), guardado.getNombre());
        return guardado;
    }

    @Override
    @Transactional
    public Proveedor actualizar(Long id, ProveedorDTO dto) {
        Proveedor p = findById(id);
        if (!p.getCedula().equals(dto.getCedula()) && proveedorRepository.existsByCedula(dto.getCedula())) {
            throw new IllegalArgumentException("Ya existe un proveedor con cédula: " + dto.getCedula());
        }
        mapFromDto(p, dto);
        Proveedor guardado = proveedorRepository.save(p);
        sincronizarItems(guardado, dto);
        return guardado;
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
