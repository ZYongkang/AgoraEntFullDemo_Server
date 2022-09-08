package com.md.mic.controller;

import com.md.common.util.ValidationUtil;
import com.md.mic.common.constants.MicStatus;
import com.md.mic.exception.UserNotFoundException;
import com.md.mic.pojos.*;
import com.md.mic.service.VoiceRoomMicService;
import com.md.mic.service.VoiceRoomService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
public class VoiceRoomController {

    @Autowired
    private VoiceRoomService voiceRoomService;

    @Autowired
    private VoiceRoomMicService voiceRoomMicService;

    @PostMapping("/voice/room/create")
    public CreateRoomResponse createVoiceRoom(
            @RequestBody @Validated CreateRoomRequest request, BindingResult result,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        ValidationUtil.validate(result);
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        boolean isPrivate = Boolean.TRUE.equals(request.getIsPrivate());
        if (isPrivate && StringUtils.isEmpty(request.getPassword())) {
            throw new IllegalArgumentException("private room password must not be null!");
        }
        Tuple2<VoiceRoomDTO, List<MicInfo>> tuples = voiceRoomService.create(user, request);

        return new CreateRoomResponse(tuples.getT1(), tuples.getT2());
    }

    @GetMapping("/voice/room/list")
    public GetRoomListResponse getRoomList(@RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit) {
        if (limit > 100) {
            throw new IllegalArgumentException("exceeded maximum paging limit");
        }
        PageInfo<RoomListDTO> pageInfo = voiceRoomService.getByPage(cursor, limit);
        return new GetRoomListResponse(pageInfo.getTotal(), pageInfo.getCursor(),
                pageInfo.getList());
    }

    @GetMapping("/voice/room/{roomId}")
    public GetVoiceRoomResponse getVoiceRoomInfo(@PathVariable("roomId") String roomId) {
        VoiceRoomDTO voiceRoomDTO = voiceRoomService.getDTOByRoomId(roomId);
        List<MicInfo> micInfo = voiceRoomMicService.getByRoomId(roomId);
        return new GetVoiceRoomResponse(voiceRoomDTO, micInfo);
    }

    @PutMapping("/voice/room/{roomId}")
    public UpdateRoomInfoResponse updateVoiceRoomInfo(@PathVariable("roomId") String roomId,
            @RequestBody UpdateRoomInfoRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        voiceRoomService.updateByRoomId(roomId, request, user.getUid());
        return new UpdateRoomInfoResponse(Boolean.TRUE);
    }

    @DeleteMapping("/voice/room/{roomId}")
    public DeleteRoomResponse deleteVoiceRoom(@PathVariable("roomId") String roomId,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        voiceRoomService.deleteByRoomId(roomId, user.getUid());
        return new DeleteRoomResponse(Boolean.TRUE);
    }

}
