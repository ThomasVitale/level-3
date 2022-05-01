package com.thomasvitale.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties(prefix = "game")
public record GameProperties(
    URI brokerUri
){}
