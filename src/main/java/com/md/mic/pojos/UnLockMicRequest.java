package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Value;

@Data
public class UnLockMicRequest {

    @JsonProperty("mic_index")
    private Integer index;
}
