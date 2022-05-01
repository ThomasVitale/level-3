package com.thomasvitale.game.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record GameTime(
    @JsonProperty("GameTimeId")
    String gameTimeId,
    @JsonProperty("SessionId")
    String sessionId,
    @JsonProperty("Level")
    String level,
    @JsonProperty("Type")
    String type,
    @JsonProperty("Time")
    LocalDateTime time
){}
