package api.astro.whats_orders_manager.modules.cliente.controller;

import api.astro.whats_orders_manager.modules.configuracion.dto.HaciendaConsultaDTO;
import api.astro.whats_orders_manager.modules.configuracion.service.HaciendaApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * «Verificar en Hacienda» del formulario del cliente (T-038, ADR-044 §5): consulta la identificación en el padrón de
 * contribuyentes (/fe/ae) y devuelve el nombre y el tipo que informa Hacienda. No guarda nada; una falla de la API
 * responde 502 con un mensaje fijo y no impide guardar el cliente. Con CLIENTE_CREAR o CLIENTE_EDITAR (sin exigir ADMIN).
 */
@RestController
@RequestMapping("/api/clientes/hacienda")
@RequiredArgsConstructor
@Slf4j
public class ClienteHaciendaRestController {

    public static final String MSG_NO_EXISTE = "La identificación no aparece en el padrón de contribuyentes de Hacienda.";
    public static final String MSG_HACIENDA =
        "No se pudo consultar a Hacienda en este momento. Puede guardar el cliente y verificar más tarde.";
    public static final String MSG_IDENTIFICACION = "Ingrese una identificación de 9 a 12 dígitos, sin guiones.";

    private final HaciendaApiService haciendaApiService;

    @GetMapping("/contribuyente/{numero}")
    @PreAuthorize("@permisoService.tieneAlgunPermisoPorCodigo(#authentication.name, 'CLIENTE_CREAR', 'CLIENTE_EDITAR')")
    public ResponseEntity<Map<String, Object>> verificar(@PathVariable String numero, Authentication authentication) {
        String limpio = numero == null ? "" : numero.replaceAll("[\\s-]", "");
        if (!limpio.matches("\\d{9,12}")) {
            return ResponseEntity.badRequest().body(Map.of("existe", false, "mensaje", MSG_IDENTIFICACION));
        }
        HaciendaConsultaDTO consulta;
        try {
            consulta = haciendaApiService.consultarContribuyente(limpio);
        } catch (RuntimeException e) {
            log.warn("Verificación en Hacienda falló para identificación de {} dígitos: {}", limpio.length(),
                e.getClass().getSimpleName());
            return bajoDeHacienda();
        }
        HaciendaConsultaDTO.ResultadoConsulta resultado = consulta == null ? null : consulta.getResultado();
        if (resultado == HaciendaConsultaDTO.ResultadoConsulta.EXITOSA) {
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("existe", true);
            cuerpo.put("nombre", consulta.getNombre());
            cuerpo.put("tipoIdentificacion", consulta.getTipoIdentificacion());
            cuerpo.put("tipoIdentificacionEnum", tipoEnum(consulta.getTipoIdentificacion()));
            return ResponseEntity.ok(cuerpo);
        }
        if (resultado == HaciendaConsultaDTO.ResultadoConsulta.NO_ENCONTRADA) {
            return ResponseEntity.ok(Map.of("existe", false, "mensaje", MSG_NO_EXISTE));
        }
        log.warn("Verificación en Hacienda sin respuesta utilizable para identificación de {} dígitos", limpio.length());
        return bajoDeHacienda();
    }

    private static ResponseEntity<Map<String, Object>> bajoDeHacienda() {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("existe", false, "mensaje", MSG_HACIENDA));
    }

    /** Códigos 01 a 04 de la nota 4 del Anexo; otro código (05, 06, desconocido) no tiene opción en el formulario. */
    static String tipoEnum(String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo.trim()) {
            case "01" -> "CEDULA_FISICA";
            case "02" -> "CEDULA_JURIDICA";
            case "03" -> "DIMEX";
            case "04" -> "NITE";
            default -> null;
        };
    }
}
