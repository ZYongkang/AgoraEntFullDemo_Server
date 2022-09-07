package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.VoiceRoomUser;
import com.md.mic.pojos.PageInfo;
import com.md.mic.pojos.UserDTO;

public interface VoiceRoomUserService extends IService<VoiceRoomUser> {

    void deleteByRoomId(String roomId);

    PageInfo<UserDTO> findPageByRoomId(String roomId, String cursor, Integer limit);

    VoiceRoomUser findByRoomIdAndUid(String roomId, String uid);

    VoiceRoomUser addVoiceRoomUser(String roomId, String uid, String password);

    void deleteVoiceRoomUser(String roomId, String uid);

    void kickVoiceRoomUser(String roomId, String ownerUid, String kickUid);
}
