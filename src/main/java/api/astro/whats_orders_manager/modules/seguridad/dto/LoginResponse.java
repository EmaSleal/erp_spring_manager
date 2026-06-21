package api.astro.whats_orders_manager.modules.seguridad.dto;

import java.util.List;

public record LoginResponse(
        Integer id,
        String nombre,
        String telefono,
        String rol,
        List<String> permisos
) {}
