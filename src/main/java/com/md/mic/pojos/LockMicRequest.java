package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class LockMicRequest {

    @JsonProperty("mic_index")
    private Integer micIndex;

    @JsonCreator
    public LockMicRequest(@JsonProperty("mic_index")Integer micIndex) {
        this.micIndex = micIndex;
    }
}
