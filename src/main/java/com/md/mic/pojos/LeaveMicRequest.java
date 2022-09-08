package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class LeaveMicRequest {

    @JsonProperty("mic_index")
    private Integer index;
}
