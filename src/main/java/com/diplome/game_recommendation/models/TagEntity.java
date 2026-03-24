package com.diplome.game_recommendation.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tags")
@Getter @Setter
public class TagEntity extends BaseEntity{
    private String name;
    private String slug;
    private Boolean keep;
    private String description;
    private String imageUrl;
    public TagEntity(){}
    public TagEntity(String name, String description, String imageUrl, Boolean keep, String slug)
    {
        this.slug = slug;
        this.keep = keep;
        this.description = description;
        this.name = name;
        this.imageUrl = imageUrl;
    }
}
