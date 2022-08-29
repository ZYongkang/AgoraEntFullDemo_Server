package com.md.mic.controller;

import com.md.mic.common.utils.ValidationUtil;
import com.md.mic.pojos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@Slf4j
@RestController
public class VoiceRoomController {

    @PostMapping("/voice/room/create")
    public CreateRoomResponse createVoiceRoom(@RequestBody @Validated CreateRoomRequest request,
            BindingResult result) {
        ValidationUtil.validate(result);
        CreateRoomResponse createRoomResponse = new CreateRoomResponse(null);
        return createRoomResponse;
    }

    @GetMapping("/voice/room/list")
    public GetRoomListResponse getRoomList(@RequestParam("cursor") String cursor,
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
