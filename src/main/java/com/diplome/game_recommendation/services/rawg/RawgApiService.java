package com.diplome.game_recommendation.services.rawg;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.diplome.game_recommendation.dtos.rawg.RawgGameDto;
import com.diplome.game_recommendation.dtos.rawg.RawgGameResponse;
import com.diplome.game_recommendation.dtos.rawg.RawgTagResponse;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

@Service
public class RawgApiService {

    private final WebClient webClient;

    private static final String API_KEY = "e129d6fa38994d7e88db556623ce617a";

    public RawgApiService(WebClient.Builder builder){

        this.webClient =
                builder.baseUrl("https://api.rawg.io/api")
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(30))
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                ))
                .build();
    }

    public RawgGameResponse getGames(int page){

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("key",API_KEY)
                        .queryParam("page",page)
                        .queryParam("page_size",40)
                        .queryParam("ordering","-rating")
                        .build())
                .retrieve()
                .bodyToMono(RawgGameResponse.class)
                .block();
    }
    public List<RawgGameDto> getGamesByTag(String slug, int page){

        var response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/games")
                        .queryParam("key",API_KEY)
                        .queryParam("page",page)
                        .queryParam("tags", slug)
                        .queryParam("page_size",40)
                        .queryParam("ordering","-rating")
                        .build())
                .retrieve()
                .bodyToMono(RawgGameResponse.class)
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
                        .queryParam("key",API_KEY)
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
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(RawgTagResponse.class)
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .block();
    }

}