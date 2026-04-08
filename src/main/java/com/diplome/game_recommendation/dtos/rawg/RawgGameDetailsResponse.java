package com.diplome.game_recommendation.dtos.rawg;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import com.diplome.game_recommendation.dtos.TagDto;

@Getter
@Setter
public class RawgGameDetailsResponse {

    private Long id;
    private String name;
    private String description;
    private String released; // "2020-09-17"
    private Double rating;
    private Integer metacritic;
    private Integer playtime;

    private String background_image;

    private List<PlatformWrapper> platforms;

    private List<TagDto> tags;
}
