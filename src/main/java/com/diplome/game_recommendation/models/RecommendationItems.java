package com.diplome.game_recommendation.models;


import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "recommendationItems")
@Getter @Setter
public class RecommendationItems extends BaseEntity{
    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;
    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private RecommendationSession session;
    private Integer rank;
    private Double score;
    public RecommendationItems(GameEntity gameEntity,RecommendationSession session, Integer rank, Double score){
        this.game = gameEntity;
        this.session = session;
        this.rank = rank;
        this.score = score;
    }
    public RecommendationItems() {
    }
}
