package com.diplome.game_recommendation.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserPreferenceDto {
    private Long tagId;
    private String tagName;
    private String tagNameRu;
    private Double preferenceWeight;
}
