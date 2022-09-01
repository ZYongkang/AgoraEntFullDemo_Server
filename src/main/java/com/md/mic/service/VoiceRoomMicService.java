package com.md.mic.service;

import com.md.mic.pojos.MicInfo;
import com.md.mic.pojos.VoiceRoomDTO;

import java.util.List;

public interface VoiceRoomMicService {

    List<MicInfo> getByRoomId(String roomId);

    List<MicInfo> getRoomMicInfo(String roomId);

    Boolean setRoomMicInfo(String roomId, String uid, Integer micIndex, Boolean inOrder);

    void initMic(VoiceRoomDTO voiceRoomDTO);

    void closeMic(String uid, String roomId, Integer micIndex);

    void openMic(String uid, String roomId, Integer index);

    void leaveMic(String uid, String roomId, Integer index);

    void muteMic(String roomId, Integer index);

    void unMuteMic(String roomId, Integer index);

    void kickUserMic(String roomId, Integer index, String uid);

    void lockMic(String roomId, Integer index);

    void unLockMic(String roomId, Integer index);

    void invite(VoiceRoomDTO roomInfo, Integer index, String uid);

    Boolean agreeInvite(String roomId, String uid, Integer micIndex);

    void exchangeMic(String roomId, Integer from, Integer to, String uid);
}
