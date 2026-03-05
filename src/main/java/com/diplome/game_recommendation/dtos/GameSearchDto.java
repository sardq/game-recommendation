package com.diplome.game_recommendation.dtos;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class GameSearchDto {

    private String query;

    private List<String> tags;

    private Integer minRating;

    private Integer maxRating;

    private Integer page;

    private Integer size;

}