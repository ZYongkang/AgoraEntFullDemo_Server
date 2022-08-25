package com.md.mic.controller;

import com.md.mic.pojos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
public class VoiceRoomMicController {

    @GetMapping("/voice/room/{roomId}/mic/apply")
    public GetMicApplyListResponse getMicApplyList(@PathVariable("roomId") String roomId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit) {
        GetMicApplyListResponse response =
                new GetMicApplyListResponse(0, null, Collections.emptyList());
        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/apply")
    public AddMicApplyResponse addMicApply(@PathVariable("roomId") String roomId,
            @RequestBody AddMicApplyRequest request) {
        AddMicApplyResponse response = new AddMicApplyResponse(Boolean.TRUE);
        return response;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/apply")
    public DeleteMicApplyResponse deleteMicApply(@PathVariable("roomId") String roomId) {
        DeleteMicApplyResponse response = new DeleteMicApplyResponse(Boolean.TRUE);
        return response;
    }

    @GetMapping("/voice/room/{roomId}/mic")
    public List<MicInfo> getRoomMicInfo(@PathVariable("roomId") String roomId) {
        return Collections.emptyList();
    }

    @PostMapping("/voice/room/{roomId}/mic/close")
    public CloseMicResponse closeMic(@PathVariable("roomId") String roomId,
            @RequestBody CloseMicRequest request) {
        CloseMicResponse response = new CloseMicResponse(Boolean.TRUE);
        return response;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/close")
    public OpenMicResponse openMic(@PathVariable("roomId") String roomId,
            @RequestBody OpenMicRequest request) {
        OpenMicResponse response = new OpenMicResponse(Boolean.TRUE);
        return response;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/leave")
    public LeaveMicResponse leaveMic(@PathVariable("roomId") String roomId,
            @RequestBody LeaveMicRequest request) {
        LeaveMicResponse response = new LeaveMicResponse(Boolean.TRUE);
        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/mute")
    public MuteMicResponse muteMic(@PathVariable("roomId") String roomId,
            @RequestBody MuteMicRequest request) {
        MuteMicResponse response = new MuteMicResponse(Boolean.TRUE);
        return response;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/mute")
    public UnMuteMicResponse unMuteMic(@PathVariable("roomId") String roomId,
            @RequestBody UnMuteMicRequest request) {
        UnMuteMicResponse response = new UnMuteMicResponse(Boolean.TRUE);
        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/exchange")
    public ExchangeMicResponse exchangeMic(@PathVariable("roomId") String roomId,
            @RequestBody ExchangeMicRequest request) {
        ExchangeMicResponse response = new ExchangeMicResponse(Boolean.TRUE);
        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/kick")
    public KickUserMicResponse kickUserMic(@PathVariable("roomId") String roomId,
            @RequestBody KickUserMicRequest request) {
        KickUserMicResponse response = new KickUserMicResponse(Boolean.TRUE);
        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/lock")
    public LockMicResponse lockMic(@PathVariable("roomId") String roomId,
            @RequestBody LockMicRequest request) {
        LockMicResponse response = new LockMicResponse(Boolean.TRUE);
        return response;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/lock")
    public UnLockMicResponse unLockMic(@PathVariable("roomId") String roomId,
            @RequestBody UnLockMicRequest request) {
        UnLockMicResponse response = new UnLockMicResponse(Boolean.TRUE);
        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/invite")
    public InviteUserOnMicResponse invite(@PathVariable("roomId") String roomId,
            @RequestBody InviteUserOnMicRequest request) {
        InviteUserOnMicResponse response = new InviteUserOnMicResponse(Boolean.TRUE);
        return response;
    }

}
