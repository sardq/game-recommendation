package com.diplome.game_recommendation.dtos.rawg;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RawgMovieResponse {
    private List<MovieItem> results;

    @Getter @Setter
    public static class MovieItem {
        private String name;
        private MovieData data;
        
        @Getter @Setter
        public static class MovieData {
            private String max; 
            @JsonProperty("480")
            private String low;
        
        }
    }
}