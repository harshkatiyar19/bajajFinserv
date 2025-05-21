package config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import service.WebhookService;

@Component
public class webhook {


    private final WebhookService webhookService;

    @Autowired
    public webhook(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        webhookService.onStartup();
    }
}