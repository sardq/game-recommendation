package com.diplome.game_recommendation.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class GameDto {

    private Long id;

    private String name;

    private String posterUrl;

    private BigDecimal rating;

    private Integer metacriticRate;

    private LocalDate releaseDate;

}