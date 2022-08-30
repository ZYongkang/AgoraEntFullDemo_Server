package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.User;
import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.CreateRoomRequest;
import com.md.mic.pojos.PageInfo;
import com.md.mic.pojos.RoomListDTO;
import com.md.mic.pojos.VoiceRoomDTO;
import com.md.service.model.entity.Users;

public interface VoiceRoomService extends IService<VoiceRoom> {

    VoiceRoomDTO create(User user, CreateRoomRequest request);

    PageInfo<RoomListDTO> getByPage(String cursor, int limit);
}
