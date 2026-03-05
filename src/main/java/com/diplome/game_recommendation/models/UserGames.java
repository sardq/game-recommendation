package com.diplome.game_recommendation.models;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "user_games")
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
    public UserGames(){}
    public UserGames(UserEntity user, GameEntity game, InteractionEnum interaction, Integer rating, LocalDateTime time){
        this.user = user;
        this.game = game;
        this.interaction = interaction;
        this.rating = rating;
        this.time = time;
    }
    public UserEntity getUser(){
        return user;
    }
    public void setUser(UserEntity user){
        this.user = user;
    }
    public void setGame(GameEntity game){
        this.game = game;
    }
    public GameEntity getGame(){
        return game;
    }
    public void setInteraction(InteractionEnum interaction){
        this.interaction = interaction;
    }
    public InteractionEnum getInteraction(){
        return interaction;
    }
    public void setRating(Integer rating){
        this.rating = rating;
    }
    public Integer getRating(){
        return rating;
    }
    public void setTime (LocalDateTime time){
        this.time = time;
    }
    public LocalDateTime getTime(){
        return time;
    }
}
