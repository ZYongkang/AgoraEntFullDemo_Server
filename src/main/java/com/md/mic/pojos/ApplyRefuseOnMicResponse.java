package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApplyRefuseOnMicResponse {

    @JsonProperty("result")
    private Boolean result;

    @JsonCreator
    public ApplyRefuseOnMicResponse(@JsonProperty("result") Boolean result) {
        this.result = result;
    }
}
