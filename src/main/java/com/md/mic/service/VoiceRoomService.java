package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.User;
import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.*;

public interface VoiceRoomService extends IService<VoiceRoom> {

    VoiceRoomDTO create(User user, CreateRoomRequest request);

    PageInfo<RoomListDTO> getByPage(String cursor, int limit);

    VoiceRoomDTO getByRoomId(String roomId);

    VoiceRoom findByRoomId(String roomId);

    void updateByRoomId(String roomId, UpdateRoomInfoRequest request, String owner);

    void deleteByRoomId(String roomId, String owner);
}
