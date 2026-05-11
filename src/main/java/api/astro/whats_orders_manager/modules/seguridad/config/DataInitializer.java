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
import api.astro.whats_orders_manager.modules.seguridad.model.Rol;
import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
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
            u.setTelefono("1234567890");
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
            c.setTipoCliente(InvoiceType.INSTITUCIONAL);
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

        ConfiguracionEmpresa config = configRepository.findById(1L).orElseGet(() -> {
            log.info("DataInitializer: creating default ConfiguracionEmpresa");
            ConfiguracionEmpresa c = new ConfiguracionEmpresa();
            c.setNombreComercial("Empresa Genérica");
            c.setNumeroIdentificacion("0000000000001");
            c.setTelefono("1234567890");
            c.setEmail("empresa@ejemplo.com");
            return configRepository.save(c);
        });

        

        
    }
}
