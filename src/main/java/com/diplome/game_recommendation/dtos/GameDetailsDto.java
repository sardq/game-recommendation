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

    private BigDecimal rating;

    private BigDecimal metacriticRate;

    private Integer playtime;

    private List<TagDto> tags;
    private Double localRating;
    private Integer localRatingCount;
    private List<String> screenshotUrls;
    private List<String> trailerUrls;
    private List<String> walkthroughUrls;
}