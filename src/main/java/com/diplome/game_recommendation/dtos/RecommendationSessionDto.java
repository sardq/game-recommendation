package com.diplome.game_recommendation.dtos;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecommendationSessionDto {

    private Long id;

    private LocalDateTime generatedAt;

    private Integer itemsCount;

}