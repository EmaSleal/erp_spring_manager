package api.astro.whats_orders_manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class WhatsOrdersManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhatsOrdersManagerApplication.class, args);
    }

}
