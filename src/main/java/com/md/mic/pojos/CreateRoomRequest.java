package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class CreateRoomRequest {

    private String name;

    @JsonProperty("is_private")
    private Boolean isPrivate;

    private String password;

    private Integer type;

    @JsonProperty("allow_free_join_mic")
    private Boolean allowFreeJoinMic;

    @JsonProperty("sound_effect")
    private String soundEffect;

}
