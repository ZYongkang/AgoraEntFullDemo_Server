package com.md.mic.service;

import com.md.mic.pojos.MicInfo;
import com.md.mic.pojos.VoiceRoomDTO;

import java.util.List;

public interface VoiceRoomMicService {

    List<MicInfo> getByRoomId(String roomId);

    List<MicInfo> getRoomMicInfo(String chatroomId);

    Boolean setRoomMicInfo(String chatroomId, String uid, Integer micIndex, Boolean inOrder);

    void initMic(String chatroomId, String ownerUid);

    void closeMic(String uid, String chatroomId, Integer micIndex);

    void openMic(String uid, String chatroomId, Integer index);

    void leaveMic(String uid, String chatroomId, Integer index);

    void muteMic(String chatroomId, Integer index);

    void unMuteMic(String chatroomId, Integer index);

    void kickUserMic(String chatroomId, Integer index, String uid);

    void lockMic(String chatroomId, Integer index);

    void unLockMic(String chatroomId, Integer index);

    void invite(VoiceRoomDTO roomInfo, Integer index, String uid);

    Boolean agreeInvite(String chatroomId, String uid, Integer micIndex);

    Boolean refuseInvite(VoiceRoomDTO roomInfo, String uid);

    void exchangeMic(String chatroomId, Integer from, Integer to, String uid);
}
