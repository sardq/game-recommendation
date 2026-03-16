package com.diplome.game_recommendation.dtos;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class GameDto {

    private Long id;

    private String name;

    private String posterUrl;

    private Double rating;

    private Integer metacriticRate;

    private LocalDate releaseDate;

}