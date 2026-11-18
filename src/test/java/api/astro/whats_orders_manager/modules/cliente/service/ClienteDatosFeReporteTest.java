package api.astro.whats_orders_manager.modules.cliente.service;

import api.astro.whats_orders_manager.modules.cliente.controller.ClienteController;
import api.astro.whats_orders_manager.modules.cliente.dto.ClienteConProblemasFe;
import api.astro.whats_orders_manager.modules.cliente.exception.DatosClienteInvalidosException;
import api.astro.whats_orders_manager.modules.cliente.model.Cliente;
import api.astro.whats_orders_manager.modules.cliente.repository.ClienteRepository;
import api.astro.whats_orders_manager.modules.cliente.service.impl.ClienteServiceImpl;
import api.astro.whats_orders_manager.modules.configuracion.enums.TipoIdentificacion;
import api.astro.whats_orders_manager.modules.facturacion.electronica.validacion.CatalogoUbicacionDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** T-038 criterio 7 (C) y regla del servicio (B): el reporte y el guardado usan el mismo validador. */
@DisplayName("T-038 — reporte de clientes con datos FE inválidos y guardado en el servicio")
class ClienteDatosFeReporteTest {

    private ClienteRepository repo;
    private ClienteServiceImpl servicio;

    private static Cliente cliente(int id, String nombre, String numero, Boolean fe) {
        Cliente c = new Cliente();
        c.setIdCliente(id);
        c.setNombre(nombre);
        c.setTipoIdentificacion(TipoIdentificacion.CEDULA_FISICA);
        c.setNumeroIdentificacion(numero);
        c.setRequiereFacturaElectronica(fe);
        return c;
    }

    @BeforeEach
    void setUp() {
        repo = mock(ClienteRepository.class);
        servicio = new ClienteServiceImpl(repo, CatalogoUbicacionDePrueba.validador());
        Cliente malo = cliente(3, "Cliente Malo", "12345678", true);
        malo.setTelefono("0000000000");
        Cliente sinFe = cliente(2, "Sin FE", "1", false);
        Cliente nulo = cliente(4, "FE nulo", null, null); // null cuenta como FE activa
        Cliente bueno = cliente(1, "Cliente Bueno", "109990999", true);
        when(repo.findAll()).thenReturn(List.of(malo, sinFe, nulo, bueno));
    }

    @Test
    @DisplayName("el reporte incluye solo clientes con FE activa (null cuenta) y al menos un problema, ordenados por id")
    void reporte() {
        List<ClienteConProblemasFe> r = servicio.clientesConDatosFeInvalidos();
        assertThat(r).extracting(x -> x.cliente().getIdCliente()).containsExactly(3, 4);
        assertThat(r.get(0).problemas()).extracting("campo").contains("numeroIdentificacion", "telefono");
        assertThat(servicio.contarClientesConDatosFeInvalidos()).isEqualTo(2);
    }

    @Test
    @DisplayName("save: FE activa inválida lanza DatosClienteInvalidosException y no guarda; sin FE guarda")
    void saveRegla() {
        assertThatThrownBy(() -> servicio.save(cliente(0, "ab", "1", true)))
                .isInstanceOf(DatosClienteInvalidosException.class)
                .satisfies(e -> assertThat(((DatosClienteInvalidosException) e).getProblemas()).isNotEmpty());
        verify(repo, never()).save(any());
        servicio.save(cliente(0, "ab", "1", false));
        verify(repo).save(any(Cliente.class));
    }

    @Test
    @DisplayName("GET /clientes?filtro=feInvalidos: solo esos clientes, con sus problemas, contador y paginación sobre la lista filtrada")
    @SuppressWarnings("unchecked")
    void controladorConFiltro() {
        ClienteController controlador = new ClienteController(servicio);
        ExtendedModelMap model = new ExtendedModelMap();
        String vista = controlador.listarClientes(0, 1, "idCliente", "asc", ClienteController.FILTRO_FE_INVALIDOS, model, null);

        assertThat(vista).isEqualTo("modules/cliente/clientes");
        assertThat(model.get("filtroFeInvalidos")).isEqualTo(true);
        assertThat(model.get("contadorFeInvalidos")).isEqualTo(2L);
        List<Cliente> pagina = (List<Cliente>) model.get("clientes");
        assertThat(pagina).extracting(Cliente::getIdCliente).containsExactly(3);
        Map<Integer, ?> problemas = (Map<Integer, ?>) model.get("problemasPorCliente");
        assertThat(problemas).containsOnlyKeys(3);
        assertThat(model.get("totalItems")).isEqualTo(2L);
        assertThat(model.get("totalPages")).isEqualTo(2);

        ExtendedModelMap segunda = new ExtendedModelMap();
        controlador.listarClientes(1, 1, "idCliente", "asc", ClienteController.FILTRO_FE_INVALIDOS, segunda, null);
        assertThat((List<Cliente>) segunda.get("clientes")).extracting(Cliente::getIdCliente).containsExactly(4);
    }

    @Test
    @DisplayName("GET /clientes sin filtro: lista normal y el contador sigue visible")
    @SuppressWarnings("unchecked")
    void controladorSinFiltro() {
        when(repo.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(cliente(1, "Cliente Bueno", "109990999", true))));
        ExtendedModelMap model = new ExtendedModelMap();
        new ClienteController(servicio).listarClientes(0, 10, "idCliente", "asc", null, model, null);
        assertThat(model.get("filtroFeInvalidos")).isEqualTo(false);
        assertThat(model.get("contadorFeInvalidos")).isEqualTo(2L);
        assertThat(model.containsAttribute("problemasPorCliente")).isFalse();
        assertThat((List<Cliente>) model.get("clientes")).hasSize(1);
    }
}
