package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class VoiceRoomDTO {

    private String roomId;

    private String channelId;

    private Long chatroomId;

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

}
