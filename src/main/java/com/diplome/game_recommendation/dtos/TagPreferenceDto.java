package com.diplome.game_recommendation.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TagPreferenceDto {
    public Long tagId;
    public Integer rating;
}