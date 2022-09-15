package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Value
public class AddGiftRequest {

    @NotBlank(message = "gift id must not be blank")
    @JsonProperty("gift_id")
    private String giftId;

    @JsonProperty("gift_name")
    private String giftName;

    @NotNull(message = "gift num must not be null")
    private Integer num;

    private Long amount;

    @JsonProperty("to_uid")
    private String toUid;

}
