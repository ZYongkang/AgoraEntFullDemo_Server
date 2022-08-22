package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class ApplyAgreeOnMicRequest {

    private String uid;

    @JsonProperty("mic_index")
    private Integer micIndex;

    @JsonCreator
    public ApplyAgreeOnMicRequest(String uid,
            @JsonProperty("mic_index") Integer micIndex) {
        this.uid = uid;
        this.micIndex = micIndex;
    }
}
