package com.diplome.game_recommendation.integration;

import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Collections;
import java.util.List;

@Service
public class PriceService {
    private final WebClient webClient;

    public PriceService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://www.cheapshark.com/api/1.0").build();
    }

    public List<GameDeal> getBestDeals(String gameName) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/deals")
                            .queryParam("title", gameName)
                            .queryParam("limit", 3)
                            .build())
                    .retrieve()
                    .bodyToFlux(GameDeal.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Data
    public static class GameDeal {
        private String storeID;
        private String salePrice;
        private String normalPrice;
        private String savings; 
    }
}