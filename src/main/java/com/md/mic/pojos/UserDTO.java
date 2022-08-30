package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.md.mic.model.EasemobUser;
import com.md.mic.model.User;
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

    public static UserDTO from(User user, EasemobUser easemobUser) {
        return UserDTO.builder()
                .uid(user.getUid())
                .chatUid(easemobUser.getChatId())
                .chatUuid(easemobUser.getChatUuid())
                .name(user.getName())
                .portrait(user.getPortrait())
                .build();
    }
}
