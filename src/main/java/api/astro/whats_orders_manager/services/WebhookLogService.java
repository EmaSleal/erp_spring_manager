package api.astro.whats_orders_manager.services;

import api.astro.whats_orders_manager.models.WebhookLog;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface WebhookLogService {
    List<WebhookLog> findAll();
    Optional<WebhookLog> findById(Integer id);
    WebhookLog save(WebhookLog webhookLog);
    void deleteById(Integer id);
    List<WebhookLog> findByPhoneNumber(String phoneNumber);
}
