package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class updateRoomInfoRequest {

    private String name;

    private String announcement;

    @JsonProperty("is_private")
    private Boolean isPrivate;

    private String password;

    @JsonProperty("use_robot")
    private Boolean useRobot;

    @JsonProperty("allowed_free_join_mic")
    private Boolean allowedFreeJoinMic;
}
