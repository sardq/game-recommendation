package com.diplome.game_recommendation.dtos.rawg;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RawgGameDto {

    private Long id;

    private String name;

    private String description;

    private String released;

    private Double rating;

    private Integer metacritic;

    private Integer playtime;

    private String background_image;

    private List<Map<String, Object>> platforms;

}