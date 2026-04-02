package com.diplome.game_recommendation.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReviewDto {
    private Long id;
    private String login;
    private String gameTitile;
    private String review;
    private Integer rating;
}
