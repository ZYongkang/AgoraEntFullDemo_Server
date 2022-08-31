package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.VoiceRoomUser;
import com.md.mic.pojos.UserDTO;

import java.util.List;

public interface VoiceRoomUserService extends IService<VoiceRoomUser> {

    List<UserDTO> getPageByRoomId(String roomId, String cursor, int limit);

    void deleteByRoomId(String roomId);
}
