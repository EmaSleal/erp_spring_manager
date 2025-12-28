package api.astro.whats_orders_manager.modules.whatsapp.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import api.astro.whats_orders_manager.modules.whatsapp.model.WebhookLog;
import api.astro.whats_orders_manager.modules.whatsapp.repository.WebhookLogRepository;

import java.util.List;
import java.util.Optional;

@Service
public class WebhookLogServiceImpl implements WebhookLogService {
    @Autowired
    private WebhookLogRepository webhookLogRepository;

    @Override
    public List<WebhookLog> findAll() { return webhookLogRepository.findAll(); }

    @Override
    public Optional<WebhookLog> findById(Integer id) { return webhookLogRepository.findById(id); }

    @Override
    public WebhookLog save(WebhookLog webhookLog) { return webhookLogRepository.save(webhookLog); }

    @Override
    public void deleteById(Integer id) { webhookLogRepository.deleteById(id); }

    @Override
    public List<WebhookLog> findByPhoneNumber(String phoneNumber) {
        return webhookLogRepository.findByPhoneNumber(phoneNumber);
    }
}