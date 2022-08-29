package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.md.mic.model.VoiceRoom;
import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@Builder(toBuilder = true)
public class VoiceRoomDTO {

    private String roomId;

    private String channelId;

    private String chatroomId;

    private String name;

    private Integer type;

    @JsonProperty("is_private")
    private Boolean isPrivate;

    @JsonProperty("allowed_free_join_mic")
    private Boolean allowFreeJoinMic;

    private UserDTO owner;

    private Long membersCount;

    private Long clickCount;

    private String announcement;

    @JsonProperty("ranking_list")
    private List<GiftRecord> rankingList;

    @JsonProperty("member_list")
    private List<UserDTO> memberList;

    public static VoiceRoomDTO from(VoiceRoom voiceRoom, UserDTO owner) {
        return new VoiceRoomDTO(voiceRoom.getRoomId(), voiceRoom.getChannelId(),
                voiceRoom.getChatroomId(), voiceRoom.getName(), voiceRoom.getType(),
                voiceRoom.getIsPrivate(), voiceRoom.getAllowedFreeJoinMic(),
                owner, 0L, 0L, voiceRoom.getAnnouncement(),
                Collections.emptyList(), Collections.emptyList());
    }

}
