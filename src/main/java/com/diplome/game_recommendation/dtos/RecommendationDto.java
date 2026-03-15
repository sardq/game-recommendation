package com.diplome.game_recommendation.dtos;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class RecommendationDto {
    private Long gameId;
    private String name;
    private String posterUrl;
    private BigDecimal rating;
    private Double recommendationScore;
}