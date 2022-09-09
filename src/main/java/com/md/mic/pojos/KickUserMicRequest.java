package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Value;

@Value
public class KickUserMicRequest {

    @JsonProperty("mic_index")
    private Integer micIndex;

    private String uid;

    @JsonCreator
    public KickUserMicRequest(@JsonProperty("mic_index") Integer micIndex, String uid) {
        this.micIndex = micIndex;
        this.uid = uid;
    }
}
