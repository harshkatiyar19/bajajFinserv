package service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import model.WebhookRequest;
import model.WebhookResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@Slf4j
public class WebhookService {

    private static final String GENERATE_WEBHOOK_URL =
            "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    private static final String FINAL_SQL_QUERY = """
            SELECT
                e1.EMP_ID,
                e1.FIRST_NAME,
                e1.LAST_NAME,
                d.DEPARTMENT_NAME,
                COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT
            FROM EMPLOYEE e1
            JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID
            LEFT JOIN EMPLOYEE e2
                ON e1.DEPARTMENT = e2.DEPARTMENT
                AND e2.DOB > e1.DOB
            GROUP BY
                e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME
            ORDER BY e1.EMP_ID DESC;
            """;


    private final WebClient webClient;

    @Autowired
    public WebhookService(WebClient webClient) {
        this.webClient = webClient;
    }

    @PostConstruct
    public void onStartup() {
        sendInitialRequest();
    }

    private void sendInitialRequest() {
        WebhookRequest request = new WebhookRequest();
        request.setName("John Doe");
        request.setRegNo("REG12347");
        request.setEmail("john@example.com");

        webClient.post()
                .uri(GENERATE_WEBHOOK_URL)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(WebhookResponse.class)
                .flatMap(this::submitFinalQuery)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .doOnError(err -> log.error("Initial request failed", err))
                .subscribe();
    }

    private Mono<Void> submitFinalQuery(WebhookResponse response) {
        String accessToken = response.getAccessToken();
        String finalUrl = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";

        log.info("AccessToken received: {}", accessToken);

        return webClient.post()
                .uri(finalUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(new FinalQueryRequest("YOUR_SQL_QUERY_HERE")) // replace with actual SQL
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info(" Successfully submitted final SQL query"))
                .doOnError(e -> log.error(" Failed to submit final SQL query", e));
    }


    private record FinalQueryRequest(String finalQuery) {}
}
