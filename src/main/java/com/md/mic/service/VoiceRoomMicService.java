package com.md.mic.service;

import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.MicInfo;

import java.util.List;

public interface VoiceRoomMicService {

    List<MicInfo> getByRoomId(String roomId);

    List<MicInfo> getRoomMicInfo(VoiceRoom voiceRoom);

    Boolean setRoomMicInfo(VoiceRoom roomInfo, String uid, Integer micIndex, boolean inOrder);

    List<MicInfo> initMic(VoiceRoom voiceRoom, Boolean isActive);

    void updateRobotMicStatus(VoiceRoom voiceRoom, Boolean isActive);

    void closeMic(String uid, String chatroomId, Integer micIndex, String roomId);

    void openMic(String uid, String chatroomId, Integer index, String roomId);

    void leaveMic(String uid, String chatroomId, Integer index, String roomId);

    void muteMic(String chatroomId, Integer index, String roomId);

    void unMuteMic(String chatroomId, Integer index, String roomId);

    void kickUserMic(String chatroomId, Integer index, String uid, String roomId);

    void lockMic(String chatroomId, Integer index, String roomId);

    void unLockMic(String chatroomId, Integer index, String roomId);

    void invite(VoiceRoom roomInfo, Integer index, String uid);

    Boolean agreeInvite(VoiceRoom roomInfo, String uid, Integer micIndex);

    Boolean refuseInvite(VoiceRoom roomInfo, String uid);

    void exchangeMic(String chatroomId, Integer from, Integer to, String uid, String roomId);
}
