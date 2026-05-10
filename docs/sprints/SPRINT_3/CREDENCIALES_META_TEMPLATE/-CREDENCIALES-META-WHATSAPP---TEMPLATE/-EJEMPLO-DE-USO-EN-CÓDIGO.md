## 📝 EJEMPLO DE USO EN CÓDIGO

```java
@Configuration
@ConfigurationProperties(prefix = "whatsapp.meta")
@Data
public class WhatsAppMetaConfig {
    private String phoneNumberId;
    private String accessToken;
    private String apiVersion;
    private String webhookVerifyToken;
}

@Service
public class MetaWhatsAppServiceImpl implements WhatsAppService {
    
    @Autowired
    private WhatsAppMetaConfig config;
    
    private String getApiUrl() {
        return String.format("https://graph.facebook.com/%s/%s/messages",
            config.getApiVersion(),
            config.getPhoneNumberId()
        );
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
```

---

**Creado:** 20 de octubre de 2025  
**Última actualización:** [ACTUALIZAR CUANDO OBTENGAS TOKENS]  
**Estado:** 📝 TEMPLATE - COMPLETAR CON VALORES REALES

⚠️ **RECORDATORIO:** Este archivo debe estar en .gitignore
