package com.diplome.game_recommendation.dtos;


import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class RecommendationDto {
    private Long gameId;
    private String name;
    private String posterUrl;
    private Integer matchPercentage;
    private Double rating;
    private Double recommendationScore;
    @JsonProperty("localRating")
    private Double localRating;
}