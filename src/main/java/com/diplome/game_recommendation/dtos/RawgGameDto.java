package com.diplome.game_recommendation.dtos;

import lombok.Data;

@Data
public class RawgGameDto {

    private Long id;

    private String name;

    private String description;

    private Integer metacritic;

    private String released;

    private Double rating;

    private String background_image;

    private String updated;

}