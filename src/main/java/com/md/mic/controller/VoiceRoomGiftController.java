package com.md.mic.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class VoiceRoomGiftController {

    @GetMapping("/voice/room/{roomId}/gift/list")
    public String listGift(@PathVariable("roomId") String roomId) {
        return "list gift " + roomId;
    }

    @PostMapping("/voice/room/{roomId}/gift/add")
    public String addGift(@PathVariable("roomId") String roomId) {
        return "add gift " + roomId;
    }
}
