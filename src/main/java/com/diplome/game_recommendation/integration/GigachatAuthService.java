package com.diplome.game_recommendation.integration;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GigachatAuthService {

    private final WebClient webClient;
    private final String authorizationKey; 
    public GigachatAuthService(WebClient.Builder builder,
                               @Value("${gigachat.authorization-key}") String authorizationKey) {
        this.webClient = builder.baseUrl("https://ngw.devices.sberbank.ru:9443/api/v2").build();
        this.authorizationKey = authorizationKey;
    }

    public String getAccessToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("scope", "GIGACHAT_API_PERS");

        Map<String, Object> response = webClient.post()
            .uri("/oauth")
            .header("Authorization", "Basic " + authorizationKey)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .header("RqUID", UUID.randomUUID().toString()) 
            .body(BodyInserters.fromFormData(formData))
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        return (String) response.get("access_token");
    }
}