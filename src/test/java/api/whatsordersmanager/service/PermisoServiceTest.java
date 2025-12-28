package api.whatsordersmanager.service;

import api.astro.whats_orders_manager.modules.seguridad.model.Usuario;
import api.astro.whats_orders_manager.modules.seguridad.enums.Permiso;
import api.astro.whats_orders_manager.modules.seguridad.repository.UsuarioRepository;
import api.astro.whats_orders_manager.modules.seguridad.repository.PermisoRepository;
import api.astro.whats_orders_manager.modules.seguridad.service.PermisoService;
import api.astro.whats_orders_manager.modules.seguridad.service.UsuarioPermisoService;
import api.astro.whats_orders_manager.services.impl.PermisoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para PermisoServiceImpl
 * 
 * Valida:
 * - Verificación de permisos por rol
 * - Validación de permisos críticos  
 * - Manejo de usuarios nulos, inactivos o bloqueados
 * - Coherencia entre roles
 * 
 * @author GitHub Copilot
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests de PermisoService - Sistema RBAC")
class PermisoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    
    @Mock
    private PermisoRepository permisoRepository;
    
    @Mock
    private UsuarioPermisoService usuarioPermisoService;
    
    private PermisoService permisoService;

    private Usuario usuarioAdmin;
    private Usuario usuarioGerente;
    private Usuario usuarioVendedor;
    private Usuario usuarioInactivo;
    private Usuario usuarioBloqueado;

    @BeforeEach
    void setUp() {
        permisoService = new PermisoServiceImpl(usuarioRepository, permisoRepository, usuarioPermisoService);
        
        // Usuario ADMIN
        usuarioAdmin = new Usuario();
        usuarioAdmin.setIdUsuario(1);
        usuarioAdmin.setNombre("Admin User");
        usuarioAdmin.setRol("ADMIN");
        usuarioAdmin.setActivo(true);
        usuarioAdmin.setBloqueado(false);

        // Usuario GERENTE
        usuarioGerente = new Usuario();
        usuarioGerente.setIdUsuario(2);
        usuarioGerente.setNombre("Gerente User");
        usuarioGerente.setRol("GERENTE");
        usuarioGerente.setActivo(true);
        usuarioGerente.setBloqueado(false);

        // Usuario VENDEDOR
        usuarioVendedor = new Usuario();
        usuarioVendedor.setIdUsuario(3);
        usuarioVendedor.setNombre("Vendedor User");
        usuarioVendedor.setRol("VENDEDOR");
        usuarioVendedor.setActivo(true);
        usuarioVendedor.setBloqueado(false);

        // Usuario INACTIVO
        usuarioInactivo = new Usuario();
        usuarioInactivo.setIdUsuario(4);
        usuarioInactivo.setNombre("Usuario Inactivo");
        usuarioInactivo.setRol("VENDEDOR");
        usuarioInactivo.setActivo(false);
        usuarioInactivo.setBloqueado(false);

        // Usuario BLOQUEADO
        usuarioBloqueado = new Usuario();
        usuarioBloqueado.setIdUsuario(5);
        usuarioBloqueado.setNombre("Usuario Bloqueado");
        usuarioBloqueado.setRol("GERENTE");
        usuarioBloqueado.setActivo(true);
        usuarioBloqueado.setBloqueado(true);
    }

    // =========================================================================
    // TESTS DE VALIDACIÓN DE ESTADO DE USUARIO
    // =========================================================================

    @Test
    @DisplayName("Usuario NULL no debe tener ningún permiso")
    void testUsuarioNull_NoTienePermisos() {
        boolean tienePermiso = permisoService.tienePermiso((Usuario) null, Permiso.FACTURA_VER);
        assertFalse(tienePermiso, "Usuario NULL no debe tener permisos");
    }

    @Test
    @DisplayName("Usuario INACTIVO no debe tener permisos")
    void testUsuarioInactivo_NoTienePermisos() {
        boolean tienePermiso = permisoService.tienePermiso(usuarioInactivo, Permiso.FACTURA_VER);
        assertFalse(tienePermiso, "Usuario inactivo no debe tener permisos");
    }

    @Test
    @DisplayName("Usuario BLOQUEADO no debe tener permisos")
    void testUsuarioBloqueado_NoTienePermisos() {
        boolean tienePermiso = permisoService.tienePermiso(usuarioBloqueado, Permiso.FACTURA_VER);
        assertFalse(tienePermiso, "Usuario bloqueado no debe tener permisos");
    }

    @Test
    @DisplayName("Usuario con ROL NULL no debe tener permisos")
    void testUsuarioSinRol_NoTienePermisos() {
        Usuario usuarioSinRol = new Usuario();
        usuarioSinRol.setIdUsuario(6);
        usuarioSinRol.setRol(null);
        usuarioSinRol.setActivo(true);
        usuarioSinRol.setBloqueado(false);

        boolean tienePermiso = permisoService.tienePermiso(usuarioSinRol, Permiso.REPORTE_DASHBOARD);
        assertFalse(tienePermiso, "Usuario sin rol no debe tener permisos");
    }

    // =========================================================================
    // TESTS DE PERMISOS - ROL ADMIN
    // =========================================================================

    @Test
    @DisplayName("ADMIN debe tener TODOS los 51 permisos")
    void testAdmin_TieneTodosLosPermisos() {
        for (Permiso permiso : Permiso.values()) {
            boolean tienePermiso = permisoService.tienePermiso(usuarioAdmin, permiso);
            assertTrue(tienePermiso, 
                "ADMIN debe tener permiso: " + permiso.getNombre());
        }
    }

    @Test
    @DisplayName("ADMIN debe tener permisos críticos")
    void testAdmin_TienePermisosCriticos() {
        assertTrue(permisoService.tienePermiso(usuarioAdmin, Permiso.USUARIO_ELIMINAR));
        assertTrue(permisoService.tienePermiso(usuarioAdmin, Permiso.USUARIO_CAMBIAR_ROL));
        assertTrue(permisoService.tienePermiso(usuarioAdmin, Permiso.CONFIG_EDITAR_EMPRESA));
        assertTrue(permisoService.tienePermiso(usuarioAdmin, Permiso.FACTURA_ELIMINAR));
        assertTrue(permisoService.tienePermiso(usuarioAdmin, Permiso.SISTEMA_BACKUP));
    }

    // =========================================================================
    // TESTS DE PERMISOS - ROL GERENTE
    // =========================================================================

    @Test
    @DisplayName("GERENTE debe tener permisos operativos completos")
    void testGerente_TienePermisosOperativos() {
        // Permisos que SÍ debe tener
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.CLIENTE_VER));
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.CLIENTE_CREAR));
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.CLIENTE_EDITAR));
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.CLIENTE_ELIMINAR));
        
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.FACTURA_VER));
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.FACTURA_CREAR));
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.FACTURA_EDITAR));
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.FACTURA_ELIMINAR));
        
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.PRODUCTO_VER));
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.PRODUCTO_CREAR));
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.PRODUCTO_EDITAR));
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.PRODUCTO_ELIMINAR));
        
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.REPORTE_VENTAS));
        assertTrue(permisoService.tienePermiso(usuarioGerente, Permiso.REPORTE_EXPORTAR_PDF));
    }

    @Test
    @DisplayName("GERENTE NO debe tener permisos administrativos críticos")
    void testGerente_NoTienePermisosAdmin() {
        // Permisos que NO debe tener
        assertFalse(permisoService.tienePermiso(usuarioGerente, Permiso.USUARIO_CREAR),
            "GERENTE no debe poder crear usuarios");
        assertFalse(permisoService.tienePermiso(usuarioGerente, Permiso.USUARIO_ELIMINAR),
            "GERENTE no debe poder eliminar usuarios");
        assertFalse(permisoService.tienePermiso(usuarioGerente, Permiso.USUARIO_CAMBIAR_ROL),
            "GERENTE no debe poder cambiar roles");
        assertFalse(permisoService.tienePermiso(usuarioGerente, Permiso.SISTEMA_BACKUP),
            "GERENTE no debe poder hacer backup");
    }

    @Test
    @DisplayName("GERENTE debe tener al menos 30 permisos")
    void testGerente_TieneCantidadCorrectaPermisos() {
        int contadorPermisos = 0;
        for (Permiso permiso : Permiso.values()) {
            if (permisoService.tienePermiso(usuarioGerente, permiso)) {
                contadorPermisos++;
            }
        }
        
        assertTrue(contadorPermisos >= 30, 
            "GERENTE debe tener al menos 30 permisos. Tiene: " + contadorPermisos);
        assertTrue(contadorPermisos < 51, 
            "GERENTE no debe tener todos los permisos. Tiene: " + contadorPermisos);
    }

    // =========================================================================
    // TESTS DE PERMISOS - ROL VENDEDOR
    // =========================================================================

    @Test
    @DisplayName("VENDEDOR debe tener permisos básicos de lectura")
    void testVendedor_TienePermisosLectura() {
        assertTrue(permisoService.tienePermiso(usuarioVendedor, Permiso.REPORTE_DASHBOARD));
        assertTrue(permisoService.tienePermiso(usuarioVendedor, Permiso.CLIENTE_VER));
        assertTrue(permisoService.tienePermiso(usuarioVendedor, Permiso.PRODUCTO_VER));
        assertTrue(permisoService.tienePermiso(usuarioVendedor, Permiso.FACTURA_VER));
    }

    @Test
    @DisplayName("VENDEDOR debe poder crear facturas")
    void testVendedor_PuedeCrearFacturas() {
        assertTrue(permisoService.tienePermiso(usuarioVendedor, Permiso.FACTURA_CREAR));
    }

    @Test
    @DisplayName("VENDEDOR NO debe poder eliminar registros")
    void testVendedor_NoTienePermisosEscritura() {
        assertFalse(permisoService.tienePermiso(usuarioVendedor, Permiso.CLIENTE_ELIMINAR));
        assertFalse(permisoService.tienePermiso(usuarioVendedor, Permiso.PRODUCTO_ELIMINAR));
        assertFalse(permisoService.tienePermiso(usuarioVendedor, Permiso.FACTURA_ELIMINAR));
    }

    @Test
    @DisplayName("VENDEDOR NO debe tener acceso a configuración y usuarios")
    void testVendedor_NoTieneAccesoAdministrativo() {
        assertFalse(permisoService.tienePermiso(usuarioVendedor, Permiso.CONFIG_VER));
        assertFalse(permisoService.tienePermiso(usuarioVendedor, Permiso.USUARIO_VER));
        assertFalse(permisoService.tienePermiso(usuarioVendedor, Permiso.CONFIG_EDITAR_EMPRESA));
    }

    @Test
    @DisplayName("VENDEDOR debe tener exactamente 15 permisos")
    void testVendedor_TieneCantidadCorrectaPermisos() {
        int contadorPermisos = 0;
        for (Permiso permiso : Permiso.values()) {
            if (permisoService.tienePermiso(usuarioVendedor, permiso)) {
                contadorPermisos++;
            }
        }
        
        assertEquals(15, contadorPermisos, 
            "VENDEDOR debe tener exactamente 15 permisos. Tiene: " + contadorPermisos);
    }

    // =========================================================================
    // TESTS DE PERMISOS CRÍTICOS
    // =========================================================================

    @Test
    @DisplayName("Solo ADMIN debe tener permiso para eliminar usuarios")
    void testSoloAdminPuedeEliminarUsuarios() {
        assertTrue(permisoService.tienePermiso(usuarioAdmin, Permiso.USUARIO_ELIMINAR));
        assertFalse(permisoService.tienePermiso(usuarioGerente, Permiso.USUARIO_ELIMINAR));
        assertFalse(permisoService.tienePermiso(usuarioVendedor, Permiso.USUARIO_ELIMINAR));
    }

    @Test
    @DisplayName("Solo ADMIN debe tener permiso para cambiar roles")
    void testSoloAdminPuedeCambiarRoles() {
        assertTrue(permisoService.tienePermiso(usuarioAdmin, Permiso.USUARIO_CAMBIAR_ROL));
        assertFalse(permisoService.tienePermiso(usuarioGerente, Permiso.USUARIO_CAMBIAR_ROL));
        assertFalse(permisoService.tienePermiso(usuarioVendedor, Permiso.USUARIO_CAMBIAR_ROL));
    }

    @Test
    @DisplayName("Solo ADMIN debe tener permiso para editar configuración de empresa")
    void testSoloAdminPuedeEditarEmpresa() {
        assertTrue(permisoService.tienePermiso(usuarioAdmin, Permiso.CONFIG_EDITAR_EMPRESA));
        assertFalse(permisoService.tienePermiso(usuarioGerente, Permiso.CONFIG_EDITAR_EMPRESA));
        assertFalse(permisoService.tienePermiso(usuarioVendedor, Permiso.CONFIG_EDITAR_EMPRESA));
    }

    // =========================================================================
    // TESTS DE ENUM PERMISO
    // =========================================================================

    @Test
    @DisplayName("Todos los permisos del enum Permiso deben ser exactamente 48")
    void testEnum_Tiene48Permisos() {
        assertEquals(48, Permiso.values().length,
            "El enum Permiso debe tener exactamente 48 permisos definidos");
    }

    // =========================================================================
    // TESTS DE CASOS EDGE
    // =========================================================================

    @Test
    @DisplayName("Usuario con rol desconocido no debe tener permisos")
    void testRolDesconocido_NoTienePermisos() {
        Usuario usuarioRolInvalido = new Usuario();
        usuarioRolInvalido.setIdUsuario(7);
        usuarioRolInvalido.setRol("ROL_INEXISTENTE");
        usuarioRolInvalido.setActivo(true);
        usuarioRolInvalido.setBloqueado(false);

        boolean tienePermiso = permisoService.tienePermiso(usuarioRolInvalido, Permiso.REPORTE_DASHBOARD);
        assertFalse(tienePermiso, "Usuario con rol desconocido no debe tener permisos");
    }

    @Test
    @DisplayName("Permiso NULL debe retornar false")
    void testPermisoNull_RetornaFalse() {
        boolean tienePermiso = permisoService.tienePermiso(usuarioAdmin, null);
        assertFalse(tienePermiso, "Permiso NULL debe retornar false");
    }

    @Test
    @DisplayName("Verificar coherencia: GERENTE tiene subset de permisos de ADMIN")
    void testCoherencia_GerenteTieneSubsetDeAdmin() {
        for (Permiso permiso : Permiso.values()) {
            if (permisoService.tienePermiso(usuarioGerente, permiso)) {
                assertTrue(permisoService.tienePermiso(usuarioAdmin, permiso),
                    "Si GERENTE tiene permiso " + permiso.getNombre() + 
                    ", ADMIN también debe tenerlo");
            }
        }
    }

    @Test
    @DisplayName("Verificar coherencia: VENDEDOR tiene subset de permisos de GERENTE")
    void testCoherencia_VendedorTieneSubsetDeGerente() {
        for (Permiso permiso : Permiso.values()) {
            if (permisoService.tienePermiso(usuarioVendedor, permiso)) {
                assertTrue(permisoService.tienePermiso(usuarioGerente, permiso),
                    "Si VENDEDOR tiene permiso " + permiso.getNombre() + 
                    ", GERENTE también debe tenerlo");
            }
        }
    }
}
