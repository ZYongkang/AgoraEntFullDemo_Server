package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.md.mic.model.UserThirdAccount;
import com.md.mic.model.User;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    private String uid;

    @JsonProperty("chat_uid")
    private String chatUid;

    @JsonIgnore
    private String chatUuid;

    private String name;

    private String portrait;

    private Integer rtcUid;

    public static UserDTO from(User user, UserThirdAccount userThirdAccount) {
        return UserDTO.builder()
                .uid(user.getUid())
                .chatUid(userThirdAccount.getChatId())
                .chatUuid(userThirdAccount.getChatUuid())
                .rtcUid(userThirdAccount.getRtcUid())
                .name(user.getName())
                .portrait(user.getPortrait())
                .build();
    }
}
