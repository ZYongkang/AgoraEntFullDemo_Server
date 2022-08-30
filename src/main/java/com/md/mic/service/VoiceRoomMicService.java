package com.md.mic.service;

import com.md.mic.pojos.MicInfo;

import java.util.List;

public interface VoiceRoomMicService {
    List<MicInfo> getByRoomId(String roomId);
}
