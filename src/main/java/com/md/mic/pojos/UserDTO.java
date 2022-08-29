package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String uid;

    @JsonProperty("chat_uid")
    private String chatUid;

    private String name;

    private String portrait;
}
