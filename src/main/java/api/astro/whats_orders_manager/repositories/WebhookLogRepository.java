package api.astro.whats_orders_manager.repositories;

import api.astro.whats_orders_manager.models.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Integer> {
    List<WebhookLog> findByPhoneNumber(String phoneNumber);
}
