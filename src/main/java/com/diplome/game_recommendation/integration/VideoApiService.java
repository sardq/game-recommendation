package com.diplome.game_recommendation.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Service
public class VideoApiService {

    private final WebClient webClient;

    @Value("${video.key}")
    private String apiKey;

    public VideoApiService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://www.googleapis.com/youtube/v3").build();
    }

    public List<String> searchVideos(String gameName, String suffix, int count) {
        if (gameName == null || gameName.isEmpty()) return Collections.emptyList();

        try {
            String searchQuery = gameName + " " + suffix;

            YoutubeSearchResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", searchQuery)
                            .queryParam("type", "video")
                            .queryParam("maxResults", count) 
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(YoutubeSearchResponse.class)
                    .block();

            if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
                return response.getItems().stream()
                        .map(item -> item.getId().getVideoId())
                        .toList();
            }
        } catch (Exception e) {
            System.err.println("Ошибка YouTube API (" + suffix + "): " + e.getMessage());
        }
        return Collections.emptyList();
    }
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class YoutubeSearchResponse {
        private List<Item> items;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private VideoId id;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VideoId {
        private String videoId;
    }
}