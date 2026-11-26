package api.astro.whats_orders_manager.modules.proveedores.enums;

import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO.ResultadoConsulta;
import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaRegimenDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** T-058 criterio 4: 1 → GENERAL, 2 → SIMPLIFICADO, 3 → NO_CONTRIBUYENTE, otro código → OTRO; sin respuesta → SIN_VERIFICAR. */
@DisplayName("T-058 — traducción del régimen de /fe/ae para el proveedor")
class RegimenHaciendaTest {

    private static HaciendaConsultaDTO respuesta(ResultadoConsulta r, Integer codigo) {
        HaciendaRegimenDTO reg = codigo == null ? null : HaciendaRegimenDTO.builder().codigo(codigo).build();
        return HaciendaConsultaDTO.builder().resultado(r).regimen(reg).build();
    }

    @Test
    @DisplayName("códigos 1, 2, 3 y otro")
    void codigos() {
        assertThat(RegimenHacienda.deConsulta(respuesta(ResultadoConsulta.EXITOSA, 1))).isEqualTo(RegimenHacienda.GENERAL);
        assertThat(RegimenHacienda.deConsulta(respuesta(ResultadoConsulta.EXITOSA, 2))).isEqualTo(RegimenHacienda.SIMPLIFICADO);
        assertThat(RegimenHacienda.deConsulta(respuesta(ResultadoConsulta.EXITOSA, 3))).isEqualTo(RegimenHacienda.NO_CONTRIBUYENTE);
        assertThat(RegimenHacienda.deConsulta(respuesta(ResultadoConsulta.EXITOSA, 9))).isEqualTo(RegimenHacienda.OTRO);
    }

    @Test
    @DisplayName("ERROR, NO_ENCONTRADA, nulo o sin régimen → SIN_VERIFICAR")
    void sinVerificar() {
        assertThat(RegimenHacienda.deConsulta(respuesta(ResultadoConsulta.ERROR, null))).isEqualTo(RegimenHacienda.SIN_VERIFICAR);
        assertThat(RegimenHacienda.deConsulta(respuesta(ResultadoConsulta.NO_ENCONTRADA, null))).isEqualTo(RegimenHacienda.SIN_VERIFICAR);
        assertThat(RegimenHacienda.deConsulta(respuesta(ResultadoConsulta.EXITOSA, null))).isEqualTo(RegimenHacienda.SIN_VERIFICAR);
        assertThat(RegimenHacienda.deConsulta(null)).isEqualTo(RegimenHacienda.SIN_VERIFICAR);
    }
}
