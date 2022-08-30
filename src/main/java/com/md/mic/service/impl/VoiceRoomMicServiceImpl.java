package com.md.mic.service.impl;

import com.md.mic.pojos.MicInfo;
import com.md.mic.service.VoiceRoomMicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class VoiceRoomMicServiceImpl implements VoiceRoomMicService {

    @Override public List<MicInfo> getByRoomId(String roomId) {
        return null;
    }
}
