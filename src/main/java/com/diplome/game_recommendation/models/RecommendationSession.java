package com.diplome.game_recommendation.models;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recommendationSession")
@Getter @Setter
public class RecommendationSession extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    LocalDateTime generatedAt;
    @OneToMany(mappedBy = "session")
    private List<RecommendationItems> items;
    public RecommendationSession(UserEntity userEntity, LocalDateTime generatedAt){
        this.generatedAt = generatedAt;
        this.user = userEntity;
    }
}
