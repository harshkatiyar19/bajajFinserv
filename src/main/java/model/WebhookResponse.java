package model;

import lombok.Data;
import java.util.List;

@Data
public class WebhookResponse {
    private String webhook;
    private String accessToken;
}
