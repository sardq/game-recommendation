package com.diplome.game_recommendation.integration;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CurrencyService {
    private final WebClient webClient = WebClient.builder().baseUrl("https://www.cbr-xml-daily.ru").build();

    public Double getUsdRate() {
        try {
            Map response = webClient.get().uri("/daily_json.js").retrieve().bodyToMono(Map.class).block();
            Map valute = (Map) response.get("Valute");
            Map usd = (Map) valute.get("USD");
            return (Double) usd.get("Value");
        } catch (Exception e) {
            return 90.0; // Значение по умолчанию, если API недоступно
        }
    }
}