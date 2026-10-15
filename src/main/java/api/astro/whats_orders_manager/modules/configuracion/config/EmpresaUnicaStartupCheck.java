package api.astro.whats_orders_manager.modules.configuracion.config;

import api.astro.whats_orders_manager.modules.configuracion.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * ADR-019 — Empresa única: al arrancar, verifica que haya exactamente una empresa activa.
 * Solo registra un ERROR; no impide el arranque.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmpresaUnicaStartupCheck {

    private final EmpresaRepository empresaRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void verificarEmpresaUnica() {
        try {
            long activas = empresaRepository.contarEmpresasActivas();
            if (activas != 1) {
                log.error("Empresa única (ADR-019): hay {} empresas activas y debe haber exactamente una. "
                        + "Los PDF, correos y cuentas bancarias dependen de la empresa activa; corrija la tabla empresa.",
                        activas);
            }
        } catch (RuntimeException e) {
            log.error("Empresa única (ADR-019): no se pudo verificar la cantidad de empresas activas", e);
        }
    }
}
