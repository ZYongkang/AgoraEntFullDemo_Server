package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Value;

@Data
public class UnMuteMicRequest {

    @JsonProperty("mic_index")
    private Integer index;
}
