package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Value;

@Data
public class ApplyAgreeOnMicResponse {

    @JsonProperty("result")
    private Boolean result;

    @JsonCreator
    public ApplyAgreeOnMicResponse(@JsonProperty("result") Boolean result) {
        this.result = result;
    }
}
