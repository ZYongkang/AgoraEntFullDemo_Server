package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Value;

@Value
public class ApplyRefuseOnMicRequest {

    private String uid;

    @JsonProperty("mic_index")
    private Integer index;

}
