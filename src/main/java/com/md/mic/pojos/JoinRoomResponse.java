package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

@Value
public class JoinRoomResponse {

    private VoiceRoomDTO room;

    @JsonProperty("mic_info")
    private List<MicInfo> micInfo;

}
