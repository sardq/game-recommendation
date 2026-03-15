package com.diplome.game_recommendation.models;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_games")
@Getter @Setter
public class UserGames extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private GameEntity game;
    @Enumerated(EnumType.STRING)
    private InteractionEnum interaction;
    private Integer rating;
    private LocalDateTime time;
    private String review;
    public UserGames(){}
    public UserGames(UserEntity user, GameEntity game, InteractionEnum interaction, Integer rating, LocalDateTime time, String review){
        this.user = user;
        this.game = game;
        this.interaction = interaction;
        this.rating = rating;
        this.time = time;
        this.review = review;
    }
}
