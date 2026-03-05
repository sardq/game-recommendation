package com.diplome.game_recommendation.models;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "userPreference")
public class UserPreference {
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
    public UserEntity getUser(){
        return user;
    }
    public void setUser(UserEntity user){
        this.user = user;
    }
    public TagEntity getTag(){
        return tag;
    }
    public void setTag(TagEntity tag){
        this.tag = tag;
    }
    public BigDecimal getPreferenceWeight(){
        return preferenceWeight;
    }
    public void setPreferenceWeight(BigDecimal preferenceWeight){
        this.preferenceWeight = preferenceWeight;
    }
}
