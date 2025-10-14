package api.astro.whats_orders_manager.controllers;

import api.astro.whats_orders_manager.models.WebhookLog;
import api.astro.whats_orders_manager.services.WebhookLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/webhooklogs")
public class WebhookLogController {
    @Autowired
    private WebhookLogService webhookLogService;

    @GetMapping
    public String listarLogs(Model model) {
        model.addAttribute("logs", webhookLogService.findAll());
        return "webhooklogs/webhooklogs";
    }

    // 🔹 Buscar logs por número de teléfono (devuelve JSON)
    @GetMapping("/buscar")
    @ResponseBody
    public List<WebhookLog> buscarPorTelefono(@RequestParam String phoneNumber) {
        return webhookLogService.findByPhoneNumber(phoneNumber);
    }
}
