package com.diplome.game_recommendation.helpers.configuration;

import org.springframework.context.annotation.Configuration;

@Configuration
public class LibrecConfig {

    public net.librec.conf.Configuration buildConfig(String dataPath) {
        net.librec.conf.Configuration conf = new net.librec.conf.Configuration();

        conf.set("data.input.path", dataPath);
        conf.set("data.model.format", "text");
        conf.set("data.column.format", "UIR");

        conf.set("rec.recommender.class", "svd");
        conf.set("rec.iterator.maximum", "20");

        return conf;
    }
}