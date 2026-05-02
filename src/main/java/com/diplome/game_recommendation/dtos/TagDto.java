package com.diplome.game_recommendation.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TagDto {
    private Long id;
    private String name;
    private String slug;
    @JsonProperty("nameRu")
    private String nameRu;
    private String imageUrl;
}
