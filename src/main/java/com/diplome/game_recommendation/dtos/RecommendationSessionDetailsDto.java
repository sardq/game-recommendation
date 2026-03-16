package com.diplome.game_recommendation.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationSessionDetailsDto {

    private Long id;

    private LocalDateTime generatedAt;

    private List<RecommendationDto> items;

}