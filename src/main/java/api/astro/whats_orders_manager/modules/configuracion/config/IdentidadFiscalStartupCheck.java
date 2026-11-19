package api.astro.whats_orders_manager.modules.configuracion.config;

import api.astro.whats_orders_manager.config.crypto.CredencialCipher;
import api.astro.whats_orders_manager.modules.configuracion.repository.ConfiguracionEmpresaRepository;
import api.astro.whats_orders_manager.modules.facturacion.electronica.model.ConfiguracionHacienda;
import api.astro.whats_orders_manager.modules.facturacion.electronica.repository.ConfiguracionHaciendaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ADR-021 — Identidad fiscal única: al arrancar, verifica que configuracion_empresa tenga exactamente una fila,
 * que haya exactamente una configuración de Hacienda activa y que su certificado exista y se pueda leer.
 * Solo registra ERROR; no impide el arranque y nunca registra credenciales.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdentidadFiscalStartupCheck {

    private final ConfiguracionEmpresaRepository configuracionEmpresaRepository;
    private final ConfiguracionHaciendaRepository configuracionHaciendaRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void verificarIdentidadFiscal() {
        try {
            long empresas = configuracionEmpresaRepository.count();
            if (empresas != 1) {
                log.error("Identidad fiscal (ADR-021): configuracion_empresa tiene {} filas y debe tener exactamente una.",
                        empresas);
            }

            List<ConfiguracionHacienda> activas;
            try {
                activas = configuracionHaciendaRepository.findByActivaTrueOrderByIdAsc();
            } catch (RuntimeException e) {
                // T-041: cargar la fila descifra sus credenciales; si no se pueden descifrar, cada envío quedará en ERROR
                log.warn("Configuración de Hacienda activa ilegible: no se pudo leer o descifrar sus credenciales. "
                        + "Revise {} (debe ser la clave con la que se guardaron); mientras tanto los envíos a "
                        + "Hacienda quedarán en ERROR con el mensaje de configuración ilegible. La aplicación "
                        + "arranca igual. Causa: {}", CredencialCipher.KEY_ENV_VAR, causaRaiz(e));
                return;
            }
            if (activas.size() != 1) {
                log.error("Identidad fiscal (ADR-021): hay {} configuraciones de Hacienda activas (ids [{}]) y debe "
                        + "haber exactamente una.", activas.size(),
                        activas.stream().map(c -> String.valueOf(c.getId())).collect(Collectors.joining(", ")));
                return;
            }

            verificarCertificado(activas.get(0));
        } catch (RuntimeException e) {
            log.error("Identidad fiscal (ADR-021): no se pudo verificar la configuración fiscal", e);
        }
    }

    private static String causaRaiz(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }

    private void verificarCertificado(ConfiguracionHacienda activa) {
        String ruta = activa.getRutaCertificado();
        if (ruta == null || ruta.isBlank()) {
            log.error("Identidad fiscal (ADR-021): la configuración de Hacienda activa (id {}, {}) no tiene ruta de "
                    + "certificado.", activa.getId(), activa.getAmbiente());
            return;
        }
        boolean legible;
        try {
            Path certificado = Path.of(ruta);
            legible = Files.isRegularFile(certificado) && Files.isReadable(certificado);
        } catch (InvalidPathException e) {
            legible = false;
        }
        if (!legible) {
            log.error("Identidad fiscal (ADR-021): el certificado de la configuración de Hacienda activa (id {}, {}) "
                    + "no existe o no se puede leer: {}", activa.getId(), activa.getAmbiente(), ruta);
        }
    }
}
