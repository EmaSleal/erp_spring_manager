package api.astro.whats_orders_manager.modules.seguridad.config;

import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.configuracion.model.Presentacion;
import api.astro.whats_orders_manager.modules.configuracion.repository.PresentacionRepository;
import api.astro.whats_orders_manager.modules.configuracion.model.ConfiguracionEmpresa;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.enums.InvoiceType;
import api.astro.whats_orders_manager.modules.producto.model.Producto;
import api.astro.whats_orders_manager.modules.producto.repository.ProductoRepository;
import api.astro.whats_orders_manager.modules.seguridad.model.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.model.Rol;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.repository.PermisoRepository;
import api.astro.whats_orders_manager.modules.seguridad.repository.RolRepository;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PresentacionRepository presentacionRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final ConfiguracionEmpresaRepository configRepository;
    private final PermisoRepository permisoRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Rol rolAdmin = rolRepository.findByCodigo("ADMIN").orElseGet(() -> {
            log.info("DataInitializer: creating ADMIN rol");
            Rol r = new Rol();
            r.setCodigo("ADMIN");
            r.setNombre("Administrador");
            r.setDescripcion("Rol con acceso total al sistema");
            r.setActivo(true);
            return rolRepository.save(r);
        });

        if (usuarioRepository.findByNombre("usuario prueba").isEmpty()) {
            log.info("DataInitializer: creating test user");
            Usuario u = new Usuario();
            u.setNombre("usuario prueba");
            u.setTelefono("50612345678");
            u.setPassword(passwordEncoder.encode("JhfKHZ2%mJMI"));
            u.setRol("ADMIN");
            u.setRolEntity(rolAdmin);
            u.setActivo(true);
            u.setBloqueado(false);
            u.setIntentosFallidos(0);
            u.setRequireCambioPassword(false);
            usuarioRepository.save(u);
        }

        Presentacion presentacionUnid = presentacionRepository.findAll().stream()
                .filter(p -> "Unid".equals(p.getCodigoUnidadFE()))
                .findFirst()
                .orElseGet(() -> {
                    log.info("DataInitializer: creating Presentacion 'Unid'");
                    Presentacion p = new Presentacion();
                    p.setNombre("Unidad");
                    p.setCodigoUnidadFE("Unid");
                    return presentacionRepository.save(p);
                });

        if (clienteRepository.count() == 0) {
            log.info("DataInitializer: creating default cliente");
            Cliente c = new Cliente();
            c.setNombre("Cliente Genérico");
            c.setIdentificacion("000000000");
            c.setEmail("cliente@ejemplo.com");
            c.setTipoCliente(InvoiceType.INSTITUCIONAL);
            c.setTelefono("0000000000");
            clienteRepository.save(c);
        }

        if (productoRepository.count() == 0) {
            log.info("DataInitializer: creating default producto");
            Producto p = new Producto();
            p.setCodigo("PROD-001");
            p.setDescripcion("Producto Genérico");
            p.setPresentacion(presentacionUnid);
            p.setActive(true);
            p.setPrecioInstitucional(BigDecimal.ZERO);
            p.setPrecioMayorista(BigDecimal.ZERO);
            p.setGravado(false);
            productoRepository.save(p);
        }

        ConfiguracionEmpresa config = configRepository.findById(1).orElseGet(() -> {
            log.info("DataInitializer: creating default ConfiguracionEmpresa");
            ConfiguracionEmpresa c = new ConfiguracionEmpresa();
            c.setNombreComercial("Empresa Genérica");
            c.setNumeroIdentificacion("0000000000001");
            c.setTelefono("1234567890");
            c.setEmail("empresa@ejemplo.com");
            c.setRazonSocial(c.getNombreComercial());
            return configRepository.save(c);
        });

        sincronizarPermisosYRoles();
    }

    private void sincronizarPermisosYRoles() {
        // Step 1: ensure every enum entry has a Permiso entity in the DB (additive only)
        Map<String, Permiso> byCode = new HashMap<>();
        for (api.astro.whats_orders_manager.modules.seguridad.enums.Permiso enumPerm
                : api.astro.whats_orders_manager.modules.seguridad.enums.Permiso.values()) {
            Permiso entity = permisoRepository.findByCodigo(enumPerm.name())
                .orElseGet(() -> {
                    log.info("DataInitializer: creating permission {}", enumPerm.name());
                    Permiso p = new Permiso();
                    p.setCodigo(enumPerm.name());
                    p.setNombre(enumPerm.getNombre());
                    p.setDescripcion(enumPerm.getDescripcion());
                    p.setCategoria(enumPerm.getCategoria());
                    p.setEsCritico(enumPerm.esCritico());
                    p.setActivo(true);
                    return permisoRepository.save(p);
                });
            byCode.put(enumPerm.name(), entity);
        }

        // Step 2: for each role in MatrizPermisos, upsert the role and add any missing permissions
        Map<String, String[]> rolesMeta = new java.util.LinkedHashMap<>();
        rolesMeta.put("VENDEDOR", new String[]{"Vendedor", "Rol de vendedor"});
        rolesMeta.put("GERENTE", new String[]{"Gerente", "Rol de gerente"});
        rolesMeta.put("ADMIN",   new String[]{"Administrador", "Rol con acceso total al sistema"});

        for (Map.Entry<String, String[]> entry : rolesMeta.entrySet()) {
            String codigo = entry.getKey();
            Rol rol = rolRepository.findByCodigo(codigo).orElseGet(() -> {
                log.info("DataInitializer: creating rol {}", codigo);
                Rol r = new Rol();
                r.setCodigo(codigo);
                r.setNombre(entry.getValue()[0]);
                r.setDescripcion(entry.getValue()[1]);
                r.setActivo(true);
                return rolRepository.save(r);
            });

            Set<api.astro.whats_orders_manager.modules.seguridad.enums.Permiso> debesTener =
                MatrizPermisos.getPermisos(codigo);
            boolean changed = false;
            for (api.astro.whats_orders_manager.modules.seguridad.enums.Permiso ep : debesTener) {
                Permiso entity = byCode.get(ep.name());
                if (entity != null && !rol.getPermisos().contains(entity)) {
                    rol.agregarPermiso(entity);
                    changed = true;
                    log.info("DataInitializer: assigning {} to rol {}", ep.name(), codigo);
                }
            }
            if (changed) rolRepository.save(rol);
        }
    }
}
