package com.diplome.game_recommendation.dtos.rawg;

import lombok.Data;

@Data
public class RawgTagDto {

    private Long id;

    private String name;
    private String description;
    private Integer games_count;
    private String slug;
    private String image_background;

}