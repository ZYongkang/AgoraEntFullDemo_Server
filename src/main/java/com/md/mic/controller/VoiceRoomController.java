package com.md.mic.controller;

import com.md.mic.common.utils.ValidationUtil;
import com.md.mic.model.User;
import com.md.mic.pojos.*;
import com.md.mic.service.VoiceRoomMicService;
import com.md.mic.service.VoiceRoomService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
public class VoiceRoomController {

    @Autowired
    private VoiceRoomService voiceRoomService;

    @Autowired
    private VoiceRoomMicService voiceRoomMicService;

    @PostMapping("/voice/room/create")
    public CreateRoomResponse createVoiceRoom(@RequestAttribute("user") User user,
            @RequestBody @Validated CreateRoomRequest request, BindingResult result) {
        ValidationUtil.validate(result);
        boolean isPrivate = Boolean.TRUE.equals(request.getIsPrivate());
        if(isPrivate && StringUtils.isEmpty(request.getPassword())){
            throw new IllegalArgumentException("private room password must not be null!");
        }
        VoiceRoomDTO roomDTO = voiceRoomService.create(user, request);
        return new CreateRoomResponse(roomDTO);
    }

    @GetMapping("/voice/room/list")
    public GetRoomListResponse getRoomList(@RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit) {
        PageInfo<RoomListDTO> pageInfo = voiceRoomService.getByPage(cursor, limit);
        return new GetRoomListResponse(pageInfo.getTotal(), pageInfo.getCursor(),
                pageInfo.getList());
    }

    @GetMapping("/voice/room/{roomId}")
    public GetVoiceRoomResponse getVoiceRoomInfo(@PathVariable("roomId") String roomId) {
        VoiceRoomDTO voiceRoomDTO = voiceRoomService.getByRoomId(roomId);
        List<MicInfo> micInfo = voiceRoomMicService.getByRoomId(roomId);
        GetVoiceRoomResponse response = new GetVoiceRoomResponse(voiceRoomDTO, micInfo);
        return response;
    }

    @PutMapping("/voice/room/{roomId}")
    public UpdateRoomInfoResponse updateVoiceRoomInfo(@PathVariable("roomId") String roomId,
            @RequestBody updateRoomInfoRequest request) {
        return new UpdateRoomInfoResponse(Boolean.TRUE);
    }

    @DeleteMapping("/voice/room/{roomId}")
    public DeleteRoomResponse deleteVoiceRoom(@PathVariable("roomId") String roomId) {
        return new DeleteRoomResponse(Boolean.TRUE);
    }

}
