package com.diplome.game_recommendation.services;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.diplome.game_recommendation.dtos.RawgGameResponse;
import com.diplome.game_recommendation.dtos.RawgTagResponse;

@Service
public class RawgApiService {

    private final WebClient webClient;

    private static final String API_KEY = "YOUR_API_KEY";

    public RawgApiService(WebClient.Builder builder){

        this.webClient =
                builder.baseUrl("https://api.rawg.io/api")
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

    public RawgTagResponse getTags(){

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tags")
                        .queryParam("key",API_KEY)
                        .build())
                .retrieve()
                .bodyToMono(RawgTagResponse.class)
                .block();
    }

}