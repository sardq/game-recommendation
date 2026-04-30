package com.diplome.game_recommendation.dtos;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class GameDto {

    private Long id;
    private Long rawgId;

    private String name;

    private String posterUrl;

    private Double rating;

    private Integer metacriticRate;

    private LocalDate releaseDate;
    @JsonProperty("localRating")
    private Double localRating;
}