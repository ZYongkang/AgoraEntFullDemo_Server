package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class RoomListDTO {

    @JsonProperty("room_id")
    private String roomId;

    @JsonProperty("channel_id")
    private String channelId;

    @JsonProperty("chatroom_id")
    private Long chatroomId;

    private String name;

    private UserDTO owner;

    @JsonProperty("is_private")
    private Boolean isPrivate;

    private Integer type;

    @JsonProperty("created_at")
    private Long createdAt;

}
