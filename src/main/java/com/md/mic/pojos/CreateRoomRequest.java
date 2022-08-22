package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Value
public class CreateRoomRequest {

    @NotBlank(message = "name not allow empty")
    private String name;

    @NotNull
    @JsonProperty("is_private")
    private Boolean isPrivate;

    private String password;

    @NotNull
    private Integer type;

    @NotNull
    @JsonProperty("allow_free_join_mic")
    private Boolean allowFreeJoinMic;

    @JsonProperty("sound_effect")
    private String soundEffect;

}
