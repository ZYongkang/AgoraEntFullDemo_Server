package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InviteAgreeOnMicResponse {

    @JsonProperty("result")
    private Boolean result;

    @JsonCreator
    public InviteAgreeOnMicResponse(@JsonProperty("result") Boolean result) {
        this.result = result;
    }
}
