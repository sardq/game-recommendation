package com.diplome.game_recommendation.dtos.rawg;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamStoreResponse {
    private Map<String, GameData> data = new HashMap<>();

    @JsonAnySetter
    public void add(String key, GameData value) {
        this.data.put(key, value);
    }

    @Data
    public static class GameData {
        private boolean success;
        private Details data;
    }

    @Data
    public static class Details {
        private List<Movie> movies;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Movie {
        private String name;
        // Старые поля (на случай, если они появятся у других игр)
        private VideoFormats mp4; 
        private VideoFormats webm;
        
        // НОВЫЕ ПОЛЯ, которые вы видите в браузере
        private String dash_h264; // Ссылка на .mpd
        private String hls_h264;  // Ссылка на .m3u8
    }

    @Data
    public static class VideoFormats {
        @JsonProperty("max")
        private String max;
        @JsonProperty("480")
        private String low;
    }
}