package com.md.mic.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class VoiceRoomUserController {

    @GetMapping("/voice/room/{roomId}/members/list")
    public String getRoomMemberList(@PathVariable("roomId") String roomId) {
        return "get room " + roomId + "member list";
    }

    @PostMapping("/voice/room/{roomId}/members/join")
    public String joinRoom(@PathVariable("roomId") String roomId) {
        return "join room " + roomId;
    }

    @DeleteMapping("/voice/room/{roomId}/members/leave")
    public String leaveRoom(@PathVariable("roomId") String roomId) {
        return "leave room " + roomId;
    }

    @DeleteMapping("/voice/room/{roomId}/members/kick")
    public String kickRoom(@PathVariable("roomId") String roomId) {
        return "kick room " + roomId;
    }
}
