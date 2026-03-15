package com.diplome.game_recommendation.dtos;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Setter @Getter
public class RecommendationListDto {
    private List<RecommendationDto> recommendations;
    private String generatedAt;
}
