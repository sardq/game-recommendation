package com.diplome.game_recommendation.dtos.rawg;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RawgStoreResponse {
    private List<StoreResult> results;

    @Getter @Setter
    public static class StoreResult {
        private Integer id;
        @JsonProperty("store_id")
        private Integer storeId; // 1 = Steam, 3 = PlayStation, 2 = Xbox
        private String url;      // Прямая ссылка на страницу в магазине
    }
}