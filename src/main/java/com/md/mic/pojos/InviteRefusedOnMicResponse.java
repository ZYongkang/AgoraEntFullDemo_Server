package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Value;

@Value
public class InviteRefusedOnMicResponse {

    @JsonProperty("result")
    private Boolean result;

    @JsonCreator
    public InviteRefusedOnMicResponse(@JsonProperty("result") Boolean result) {
        this.result = result;
    }
}
