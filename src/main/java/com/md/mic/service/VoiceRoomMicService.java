package com.md.mic.service;

import com.md.mic.pojos.CloseMicRequest;
import com.md.mic.pojos.MicInfo;

import java.util.List;

public interface VoiceRoomMicService {

    List<MicInfo> getByRoomId(String roomId);

    List<MicInfo> getRoomMicInfo(String roomId);

    Boolean setRoomMicInfoInOrder(String roomId, String uid, Integer micIndex);

    void closeMic(String uid, String roomId,Integer micIndex);

    void openMic(String uid, String roomId, Integer index);

    void leaveMic(String uid, String roomId, Integer index);

    void muteMic(String roomId, Integer index);

    void unMuteMic(String roomId, Integer index);
}
