package com.diplome.game_recommendation.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class GameDetailsDto {

    private Long id;

    private String name;

    private String description;

    private LocalDate releaseDate;

    private List<String> platforms;

    private String posterUrl;

    private String developers;

    private String publishers;

    private BigDecimal rating;

    private BigDecimal metacriticRate;

    private Integer playtime;

    private List<String> tags;

}