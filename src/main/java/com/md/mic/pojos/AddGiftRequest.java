package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class AddGiftRequest {

    @JsonProperty("gift_id")
    private String giftId;

    private Integer num;

    @JsonProperty("to_uid")
    private String toUid;

}
