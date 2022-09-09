package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class OpenMicRequest {

    @JsonProperty("mic_index")
    private Integer micIndex;

    @JsonCreator
    public OpenMicRequest(@JsonProperty("mic_index") Integer micIndex) {
        this.micIndex = micIndex;
    }
}
