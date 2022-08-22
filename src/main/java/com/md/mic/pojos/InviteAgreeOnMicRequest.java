package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
public class InviteAgreeOnMicRequest {

    @NotNull(message = "uid must not be null")
    private String uid;

    @JsonProperty("mic_index")
    private Integer micIndex;

    @JsonCreator
    public InviteAgreeOnMicRequest(String uid,
            @JsonProperty("mic_index") Integer micIndex) {
        this.uid = uid;
        this.micIndex = micIndex;
    }
}
