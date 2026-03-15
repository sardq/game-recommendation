package com.diplome.game_recommendation.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "game_tags")
@Getter @Setter
public class GameTag {

    @EmbeddedId
    private GameTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId")
    @JoinColumn(name = "game_id")
    private GameEntity game;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id")
    private TagEntity tag;

    public GameTag() {}

    public GameTag(GameEntity game, TagEntity tag) {
        this.game = game;
        this.tag = tag;
        this.id = new GameTagId(game.getId(), tag.getId());
    }

    
}