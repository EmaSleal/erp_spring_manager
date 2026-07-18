package api.astro.whats_orders_manager.modules.whatsapp.repository;

import api.astro.whats_orders_manager.modules.whatsapp.model.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Integer> {
    List<WebhookLog> findByPhoneNumber(String phoneNumber);
    Long countByTimestampBetween(LocalDateTime inicio, LocalDateTime fin);
}
