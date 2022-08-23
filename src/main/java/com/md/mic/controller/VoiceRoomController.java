package com.md.mic.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class VoiceRoomController {

    @PostMapping("/voice/room/create")
    public String createVoiceRoom() {
        return "create room";
    }

    @GetMapping("/voice/room/list")
    public String getRoomList() {
        return "get room list";
    }

    @GetMapping("/voice/room/{roomId}")
    public String getVoiceRoomInfo(@PathVariable("roomId") String roomId) {
        return "get room info " + roomId;
    }

    @DeleteMapping("/voice/room/{roomId}")
    public String deleteVoiceRoom(@PathVariable("roomId") String roomId) {
        return "delete room " + roomId;
    }
}
