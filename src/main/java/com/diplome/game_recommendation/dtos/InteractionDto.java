package com.diplome.game_recommendation.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InteractionDto {
    private Long gameId;
    private String interactionType;
    private Integer rating;
}
