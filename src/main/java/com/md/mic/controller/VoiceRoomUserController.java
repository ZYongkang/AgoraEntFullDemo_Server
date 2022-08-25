package com.md.mic.controller;

import com.md.mic.pojos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@Slf4j
@RestController
public class VoiceRoomUserController {

    @GetMapping("/voice/room/{roomId}/members/list")
    public GetRoomUserListResponse getRoomMemberList(@PathVariable("roomId") String roomId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit) {
        GetRoomUserListResponse response =
                new GetRoomUserListResponse(0, null, Collections.emptyList());
        return response;
    }

    @PostMapping("/voice/room/{roomId}/members/join")
    public JoinRoomResponse joinRoom(@PathVariable("roomId") String roomId,
            @RequestBody JoinRoomRequest request) {
        JoinRoomResponse response = new JoinRoomResponse(null);
        return response;
    }

    @DeleteMapping("/voice/room/{roomId}/members/leave")
    public LeaveRoomResponse leaveRoom(@PathVariable("roomId") String roomId) {
        LeaveRoomResponse response = new LeaveRoomResponse(Boolean.TRUE);
        return response;
    }

    @DeleteMapping("/voice/room/{roomId}/members/kick")
    public KickRoomResponse kickRoom(@PathVariable("roomId") String roomId,
            @RequestBody KickRoomRequest request) {
        KickRoomResponse response = new KickRoomResponse(Boolean.TRUE);
        return response;
    }

}
