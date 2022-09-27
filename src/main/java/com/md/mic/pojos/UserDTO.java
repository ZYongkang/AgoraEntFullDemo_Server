package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.md.mic.model.User;
import com.md.mic.model.UserThirdAccount;
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

    @JsonProperty("rtc_uid")
    private Integer rtcUid;

    @JsonProperty("mic_index")
    private Integer micIndex;

    public static UserDTO from(User user, UserThirdAccount userThirdAccount, Integer micIndex) {
        return UserDTO.builder()
                .uid(user.getUid())
                .chatUid(userThirdAccount.getChatId())
                .chatUuid(userThirdAccount.getChatUuid())
                .rtcUid(userThirdAccount.getRtcUid())
                .name(user.getName())
                .micIndex(micIndex)
                .portrait(user.getPortrait())
                .build();
    }

}
