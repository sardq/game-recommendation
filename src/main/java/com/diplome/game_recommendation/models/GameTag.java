package com.diplome.game_recommendation.models;

import jakarta.persistence.*;

@Entity
@Table(name = "game_tags")
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

    public GameEntity getGame() {
        return game;
    }

    public void setGame(GameEntity game) {
        this.game = game;
    }

    public TagEntity getTag() {
        return tag;
    }

    public void setTag(TagEntity tag) {
        this.tag = tag;
    }
}