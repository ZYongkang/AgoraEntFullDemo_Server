package com.md.mic.controller;

import com.md.mic.common.utils.ValidationUtil;
import com.md.mic.pojos.*;
import com.md.mic.service.VoiceRoomService;
import com.md.service.model.entity.Users;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@Slf4j
@RestController
public class VoiceRoomController {

    @Autowired
    private VoiceRoomService voiceRoomService;

    @PostMapping("/voice/room/create")
    public CreateRoomResponse createVoiceRoom(@RequestAttribute("user") Users user,
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
        return new GetRoomListResponse(0, null, Collections.emptyList());
    }

    @GetMapping("/voice/room/{roomId}")
    public GetVoiceRoomResponse getVoiceRoomInfo(@PathVariable("roomId") String roomId) {
        GetVoiceRoomResponse response = new GetVoiceRoomResponse(null, null);
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
