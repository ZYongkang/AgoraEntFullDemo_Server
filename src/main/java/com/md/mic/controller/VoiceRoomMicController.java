package com.md.mic.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class VoiceRoomMicController {

    @GetMapping("/voice/room/{roomId}/mic/apply")
    public String getMicApplyList(@PathVariable("roomId") String roomId) {
        return "get mic apply list " + roomId;
    }

    @PostMapping("/voice/room/{roomId}/mic/apply")
    public String addMicApply(@PathVariable("roomId") String roomId) {
        return "add mic apply " + roomId;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/apply")
    public String deleteMicApply(@PathVariable("roomId") String roomId) {
        return "delete mic apply " + roomId;
    }

    @GetMapping("/voice/room/{roomId}/mic")
    public String getRoomMicInfo(@PathVariable("roomId") String roomId) {
        return "get room mic info " + roomId;
    }

    @PostMapping("/voice/room/{roomId}/mic/close")
    public String closeMic(@PathVariable("roomId") String roomId) {
        return "close mic " + roomId;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/close")
    public String openMic(@PathVariable("roomId") String roomId) {
        return "open mic " + roomId;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/leave")
    public String leaveMic(@PathVariable("roomId") String roomId) {
        return "leave mic " + roomId;
    }

    @PostMapping("/voice/room/{roomId}/mic/mute")
    public String muteMic(@PathVariable("roomId") String roomId) {
        return "mute mic " + roomId;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/mute")
    public String unMuteMic(@PathVariable("roomId") String roomId) {
        return "unmute mic " + roomId;
    }

    @PostMapping("/voice/room/{roomId}/mic/exchange")
    public String exchangeMic(@PathVariable("roomId") String roomId) {
        return "exchange mic location " + roomId;
    }

    @PostMapping("/voice/room/{roomId}/mic/kick")
    public String kickUserMic(@PathVariable("roomId") String roomId) {
        return "kick user mic " + roomId;
    }

    @PostMapping("/voice/room/{roomId}/mic/lock")
    public String lockMic(@PathVariable("roomId") String roomId) {
        return "lock mic " + roomId;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/lock")
    public String unLockMic(@PathVariable("roomId") String roomId) {
        return "unlock mic " + roomId;
    }

    @PostMapping("/voice/room/{roomId}/mic/invite")
    public String invite(@PathVariable("roomId") String roomId) {
        return "invite user " + roomId;
    }
}
