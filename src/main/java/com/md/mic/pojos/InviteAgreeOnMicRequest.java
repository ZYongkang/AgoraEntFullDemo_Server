package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class InviteAgreeOnMicRequest {

    private String uid;

    @JsonProperty("mic_index")
    private Integer index;


}
