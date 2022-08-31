package com.md.mic.controller;

import com.md.mic.model.User;
import com.md.mic.pojos.*;
import com.md.mic.service.MicApplyUserService;
import com.md.mic.service.VoiceRoomMicService;
import com.md.mic.service.VoiceRoomService;
import com.md.service.model.dto.RoomInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
public class VoiceRoomMicController {

    @Autowired
    private MicApplyUserService micApplyUserService;

    @Autowired
    private VoiceRoomMicService voiceRoomMicService;

    @Autowired
    private VoiceRoomService voiceRoomService;

    @GetMapping("/voice/room/{roomId}/mic/apply")
    public GetMicApplyListResponse getMicApplyList(@PathVariable("roomId") String roomId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit) {
        GetMicApplyListResponse response =
                new GetMicApplyListResponse(0, null, Collections.emptyList());
        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/apply")
    public AddMicApplyResponse addMicApply(@RequestAttribute("user") User user,@PathVariable("roomId") String roomId,
            @RequestBody AddMicApplyRequest request) {
        AddMicApplyResponse response = new AddMicApplyResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo=voiceRoomService.getByRoomId(roomId);
        if(roomInfo==null){
            throw new IllegalArgumentException("room is not be found!");
        }
        micApplyUserService.addMicApply(user.getUid(),roomId,request);
        return response;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/apply")
    public DeleteMicApplyResponse deleteMicApply(@RequestAttribute("user") User user,@PathVariable("roomId") String roomId) {
        DeleteMicApplyResponse response = new DeleteMicApplyResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo=voiceRoomService.getByRoomId(roomId);
        if(roomInfo==null){
            throw new IllegalArgumentException("room is not be found!");
        }
        micApplyUserService.deleteMicApply(user.getUid(),roomId);
        return response;
    }

    @GetMapping("/voice/room/{roomId}/mic")
    public List<MicInfo> getRoomMicInfo(@PathVariable("roomId") String roomId) {
        VoiceRoomDTO roomInfo=voiceRoomService.getByRoomId(roomId);
        if(roomInfo==null){
            throw new IllegalArgumentException("room is not be found!");
        }
        return voiceRoomMicService.getRoomMicInfo(roomId);
    }

    //闭麦
    @PostMapping("/voice/room/{roomId}/mic/close")
    public CloseMicResponse closeMic(@RequestAttribute("user") User user,@PathVariable("roomId") String roomId,
            @RequestBody CloseMicRequest request) {
        CloseMicResponse response = new CloseMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo=voiceRoomService.getByRoomId(roomId);
        if(roomInfo==null){
            throw new IllegalArgumentException("room is not be found!");
        }
        this.voiceRoomMicService.closeMic(user.getUid(),roomId,request.getIndex());
        return response;
    }

    //取消关麦、开麦
    @DeleteMapping("/voice/room/{roomId}/mic/close")
    public OpenMicResponse openMic(@RequestAttribute("user") User user,@PathVariable("roomId") String roomId,
            @RequestBody OpenMicRequest request) {
        OpenMicResponse response = new OpenMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo=voiceRoomService.getByRoomId(roomId);
        if(roomInfo==null){
            throw new IllegalArgumentException("room is not be found!");
        }
        this.voiceRoomMicService.openMic(user.getUid(),roomId,request.getIndex());
        return response;
    }

    //下麦
    @DeleteMapping("/voice/room/{roomId}/mic/leave")
    public LeaveMicResponse leaveMic(@RequestAttribute("user") User user,@PathVariable("roomId") String roomId,
            @RequestBody LeaveMicRequest request) {
        LeaveMicResponse response = new LeaveMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo=voiceRoomService.getByRoomId(roomId);
        if(roomInfo==null){
            throw new IllegalArgumentException("room is not be found!");
        }
        this.voiceRoomMicService.leaveMic(user.getUid(),roomId,request.getIndex());
        return response;
    }

    //禁言麦位
    @PostMapping("/voice/room/{roomId}/mic/mute")
    public MuteMicResponse muteMic(@RequestAttribute("user") User user,@PathVariable("roomId") String roomId,
            @RequestBody MuteMicRequest request) {
        MuteMicResponse response = new MuteMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo=voiceRoomService.getByRoomId(roomId);
        if(roomInfo==null){
            throw new IllegalArgumentException("room is not be found!");
        }
        if(!roomInfo.getOwner().getUid().equals(user.getUid())){
            throw new IllegalArgumentException("only the admin can mute mic");
        }
        this.voiceRoomMicService.muteMic(roomId,request.getIndex());

        return response;
    }

    //取消禁言
    @DeleteMapping("/voice/room/{roomId}/mic/mute")
    public UnMuteMicResponse unMuteMic(@RequestAttribute("user") User user,@PathVariable("roomId") String roomId,
            @RequestBody UnMuteMicRequest request) {
        UnMuteMicResponse response = new UnMuteMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo=voiceRoomService.getByRoomId(roomId);
        if(roomInfo==null){
            throw new IllegalArgumentException("room is not be found!");
        }
        if(!roomInfo.getOwner().getUid().equals(user.getUid())){
            throw new IllegalArgumentException("only the admin can mute mic");
        }
        this.voiceRoomMicService.unMuteMic(roomId,request.getIndex());

        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/exchange")
    public ExchangeMicResponse exchangeMic(@PathVariable("roomId") String roomId,
            @RequestBody ExchangeMicRequest request) {
        ExchangeMicResponse response = new ExchangeMicResponse(Boolean.TRUE);
        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/kick")
    public KickUserMicResponse kickUserMic(@RequestAttribute("user") User user,@PathVariable("roomId") String roomId,
            @RequestBody KickUserMicRequest request) {
        KickUserMicResponse response = new KickUserMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo=voiceRoomService.getByRoomId(roomId);
        if(roomInfo==null){
            throw new IllegalArgumentException("room is not be found!");
        }
        if(!roomInfo.getOwner().getUid().equals(user.getUid())){
            throw new IllegalArgumentException("only the admin can mute mic");
        }
        this.voiceRoomMicService.unMuteMic(roomId,request.getIndex());

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

    //群主同意上麦申请
    @PostMapping("/voice/room/{roomId}/mic/apply/agree")
    public ApplyAgreeOnMicResponse agreeApply(@PathVariable("roomId") String roomId,
            @RequestBody ApplyAgreeOnMicRequest request) {
        ApplyAgreeOnMicResponse response = new ApplyAgreeOnMicResponse(Boolean.TRUE);
        Boolean result=micApplyUserService.agreeApply(roomId,request.getUid());
        response.setResult(result);
        return response;
    }


}
