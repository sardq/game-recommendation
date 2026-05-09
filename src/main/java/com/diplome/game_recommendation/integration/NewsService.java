package com.diplome.game_recommendation.integration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Collections;
import java.util.List;

@Service
public class NewsService {
    private final WebClient webClient;
    @Value("${news.key}") private String apiKey;
    public NewsService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://newsapi.org/v2").build();
    }

    public List<Article> getLatestNews(String gameName) {
        String searchQuery = "игра " + gameName;
        try {
            NewsResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/everything")
                            .queryParam("q", searchQuery)
                            .queryParam("language", "ru")
                            .queryParam("sortBy", "publishedAt")
                            .queryParam("pageSize", 3)
                            .queryParam("apiKey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(NewsResponse.class)
                    .block();

            return response != null ? response.getArticles() : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Data static class NewsResponse { private List<Article> articles; }
    @Data public static class Article {
        private String title;
        private String description;
        private String url;
        private String urlToImage;
    }
}