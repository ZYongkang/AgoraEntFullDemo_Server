package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    private String uid;

    @JsonProperty("chat_uid")
    private String chatUid;

    @JsonProperty("chat_uuid")
    private String chatUuid;

    private String name;

    private String portrait;
}
