package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.*;
import reactor.util.function.Tuple2;

import java.util.List;

public interface VoiceRoomService extends IService<VoiceRoom> {

    Tuple2<VoiceRoomDTO, List<MicInfo>> create(UserDTO owner, CreateRoomRequest request);

    PageInfo<RoomListDTO> getByPage(String cursor, int limit);

    VoiceRoomDTO getDTOByRoomId(String roomId);

    VoiceRoom findByRoomId(String roomId);

    void updateByRoomId(String roomId, UpdateRoomInfoRequest request, String owner);

    void deleteByRoomId(String roomId, String owner);

    public Long getClickCount(String roomId);

    public Long getMemberCount(String roomId);
}
