package com.diplome.game_recommendation.integration;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.diplome.game_recommendation.dtos.rawg.RawgGameDetailsResponse;
import com.diplome.game_recommendation.dtos.rawg.RawgGameDto;
import com.diplome.game_recommendation.dtos.rawg.RawgGameResponse;
import com.diplome.game_recommendation.dtos.rawg.RawgMovieResponse;
import com.diplome.game_recommendation.dtos.rawg.RawgScreenshotsResponse;
import com.diplome.game_recommendation.dtos.rawg.RawgStoreResponse;
import com.diplome.game_recommendation.dtos.rawg.RawgTagResponse;
import com.diplome.game_recommendation.dtos.rawg.SteamStoreResponse;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Service
public class RawgApiService {

    private final WebClient webClient;

    @Value("${rawg.key}")
    private String apiKey;

    public RawgApiService(WebClient.Builder builder){
        int size = 16 * 1024 * 1024;
        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer
                    .defaultCodecs()
                    .maxInMemorySize(size))
            .build();
        this.webClient =
                builder.baseUrl("https://api.rawg.io/api")
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(120))
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                ))
                .exchangeStrategies(strategies)
                .build();
    }

    public RawgGameResponse getGames(int page){

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("key",apiKey)
                        .queryParam("page",page)
                        .queryParam("page_size",40)
                        .queryParam("ordering","-rating")
                        .build())
                .retrieve()
                .bodyToMono(RawgGameResponse.class)
                .block();
    }
    public RawgGameDetailsResponse getGameDetails(Long id) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/{id}")
                        .queryParam("key", apiKey)
                        .build(id))
                .retrieve()
                .bodyToMono(RawgGameDetailsResponse.class)
                .block();
        }
    public List<RawgGameDto> getGamesByTag(String slug, int page){

        var response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("key",apiKey)
                        .queryParam("page",page)
                        .queryParam("tags", slug)
                        .queryParam("page_size",40)
                        .queryParam("ordering","-rating")
                        .build())
                .retrieve()
                .bodyToMono(RawgGameResponse.class)
                .timeout(Duration.ofSeconds(120)) 
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .block();
                if (response != null && response.getResults() != null) {
                return response.getResults();
        }
        return Collections.emptyList();
    }
    public RawgGameResponse searchGames(String search){

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("key",apiKey)
                        .queryParam("search",search)
                        .queryParam("page_size",20)
                        .build())
                .retrieve()
                .bodyToMono(RawgGameResponse.class)
                .block();
    }

    public RawgTagResponse getTags(int page){

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tags")
                        .queryParam("page",page)
                        .queryParam("page_size",40)
                        .queryParam("key",apiKey)
                        .build())
                .retrieve()
                .bodyToMono(RawgTagResponse.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .block();
    }
    public List<String> getGameScreenshots(Long id) {
        RawgScreenshotsResponse res = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games/{id}/screenshots")
                        .queryParam("key", apiKey)
                        .build(id))
                .retrieve()
                .bodyToMono(RawgScreenshotsResponse.class)
                .block();
        
        if (res != null && res.getResults() != null) {
                return res.getResults().stream().map(s -> s.getImage()).toList();
        }
        return Collections.emptyList();
        }

        public String getGameTrailer(Long id) {
                try {
                        RawgMovieResponse res = webClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .path("/games/{id}/movies")
                                        .queryParam("key", apiKey)
                                        .build(id))
                                .retrieve()
                                .bodyToMono(RawgMovieResponse.class)
                                .block();

                        if (res != null && res.getResults() != null && !res.getResults().isEmpty()) {
                        // Берем 'max' качество первого трейлера из списка
                        String videoUrl = res.getResults().get(0).getData().getMax();
                        System.out.println("Найден трейлер для игры " + id + ": " + videoUrl);
                        return videoUrl;
                        } else {
                        System.out.println("У игры " + id + " нет доступных трейлеров в RAWG API");
                        }
                } catch (Exception e) {
                        System.err.println("Ошибка при получении трейлера: " + e.getMessage());
                }
        return null;
        }
        public List<String> getSteamTrailers(String steamAppId) {
                if (steamAppId == null) return Collections.emptyList();
                try {
                        SteamStoreResponse response = webClient.get()
                                .uri("https://store.steampowered.com/api/appdetails?appids=" + steamAppId)
                                .retrieve()
                                .bodyToMono(SteamStoreResponse.class)
                                .block();

                        if (response != null && response.getData().containsKey(steamAppId)) {
                        var movieData = response.getData().get(steamAppId);
                        if (movieData.isSuccess() && movieData.getData() != null && movieData.getData().getMovies() != null) {
                                
                                return movieData.getData().getMovies().stream()
                                .map(m -> {
                                        // 1. Пробуем старый добрый MP4
                                        if (m.getMp4() != null) return m.getMp4().getMax();
                                        
                                        // 2. Если его нет, берем HLS (он лучше поддерживается браузерами, чем DASH)
                                        if (m.getHls_h264() != null) return m.getHls_h264();
                                        
                                        // 3. Или DASH
                                        if (m.getDash_h264() != null) return m.getDash_h264();
                                        
                                        return null;
                                })
                                .filter(Objects::nonNull)
                                .toList();
                        }
                        }
                } catch (Exception e) {
                        System.err.println("Steam API Error: " + e.getMessage());
                }
                return Collections.emptyList();
                }
        public List<RawgStoreResponse.StoreResult> getGameStores(Long gameId) {
                RawgStoreResponse res = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/games/{id}/stores")
                                .queryParam("key", apiKey)
                                .build(gameId))
                        .retrieve()
                        .bodyToMono(RawgStoreResponse.class)
                        .block();

                return res != null ? res.getResults() : Collections.emptyList();
                }
}