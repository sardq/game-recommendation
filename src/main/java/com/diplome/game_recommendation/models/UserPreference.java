package com.diplome.game_recommendation.models;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "userPreference")
@Getter @Setter
public class UserPreference extends BaseEntity{
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    private TagEntity tag;

    @Column(nullable = false)
    private BigDecimal preferenceWeight;
    public UserPreference(){}
    public UserPreference(UserEntity user, TagEntity tag, BigDecimal preferenceWeight){
        this.user =user;
        this.tag = tag;
        this.preferenceWeight = preferenceWeight;
    }
}
