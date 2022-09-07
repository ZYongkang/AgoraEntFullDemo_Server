package com.md.mic.controller;

import com.md.mic.exception.UserNotFoundException;
import com.md.mic.pojos.*;
import com.md.mic.service.MicApplyUserService;
import com.md.mic.service.VoiceRoomMicService;
import com.md.mic.service.VoiceRoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
        PageInfo<MicApplyDTO> pageInfo = micApplyUserService.getByPage(roomId, cursor, limit);
        return new GetMicApplyListResponse(pageInfo.getTotal(), pageInfo.getCursor(),
                pageInfo.getList());
    }

    @PostMapping("/voice/room/{roomId}/mic/apply")
    public AddMicApplyResponse addMicApply(@PathVariable("roomId") String roomId,
            @RequestBody AddMicApplyRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        Boolean result = micApplyUserService.addMicApply(user.getUid(), roomInfo, request);
        return new AddMicApplyResponse(result);
    }

    @DeleteMapping("/voice/room/{roomId}/mic/apply")
    public DeleteMicApplyResponse deleteMicApply(@PathVariable("roomId") String roomId,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        micApplyUserService.deleteMicApply(user.getUid(), roomId);
        return new DeleteMicApplyResponse(Boolean.TRUE);
    }

    @GetMapping("/voice/room/{roomId}/mic")
    public List<MicInfo> getRoomMicInfo(@PathVariable("roomId") String roomId) {
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        return voiceRoomMicService.getRoomMicInfo(roomInfo.getChatroomId());
    }

    //闭麦
    @PostMapping("/voice/room/{roomId}/mic/close")
    public CloseMicResponse closeMic(@PathVariable("roomId") String roomId,
            @RequestBody CloseMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        this.voiceRoomMicService.closeMic(user.getUid(), roomInfo.getChatroomId(), request.getIndex());
        return new CloseMicResponse(Boolean.TRUE);
    }

    //取消关麦、开麦
    @DeleteMapping("/voice/room/{roomId}/mic/close")
    public OpenMicResponse openMic(@PathVariable("roomId") String roomId,
            @RequestBody OpenMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        OpenMicResponse response = new OpenMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        this.voiceRoomMicService.openMic(user.getUid(), roomInfo.getChatroomId(),
                request.getIndex());
        return response;
    }

    //下麦
    @DeleteMapping("/voice/room/{roomId}/mic/leave")
    public LeaveMicResponse leaveMic(@PathVariable("roomId") String roomId,
            @RequestBody LeaveMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        LeaveMicResponse response = new LeaveMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        this.voiceRoomMicService.leaveMic(user.getUid(), roomInfo.getChatroomId(),
                request.getIndex());
        return response;
    }

    //禁言麦位
    @PostMapping("/voice/room/{roomId}/mic/mute")
    public MuteMicResponse muteMic(@PathVariable("roomId") String roomId,
            @RequestBody MuteMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        MuteMicResponse response = new MuteMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        if (!roomInfo.getOwner().getUid().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can mute mic");
        }
        this.voiceRoomMicService.muteMic(roomInfo.getChatroomId(), request.getIndex());

        return response;
    }

    //取消禁言
    @DeleteMapping("/voice/room/{roomId}/mic/mute")
    public UnMuteMicResponse unMuteMic(@PathVariable("roomId") String roomId,
            @RequestBody UnMuteMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        UnMuteMicResponse response = new UnMuteMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        if (!roomInfo.getOwner().getUid().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can unmute mic");
        }
        this.voiceRoomMicService.unMuteMic(roomInfo.getChatroomId(), request.getIndex());

        return response;
    }

    //交换麦位
    @PostMapping("/voice/room/{roomId}/mic/exchange")
    public ExchangeMicResponse exchangeMic(@PathVariable("roomId") String roomId,
            @RequestBody ExchangeMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        ExchangeMicResponse response = new ExchangeMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        this.voiceRoomMicService
                .exchangeMic(roomInfo.getChatroomId(), request.getFrom(), request.getTo(),
                        user.getUid());
        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/kick")
    public KickUserMicResponse kickUserMic(
            @PathVariable("roomId") String roomId,
            @RequestBody KickUserMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        KickUserMicResponse response = new KickUserMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        if (!roomInfo.getOwner().getUid().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can kick mic");
        }
        this.voiceRoomMicService.kickUserMic(roomInfo.getChatroomId(), request.getIndex(),
                request.getUid());

        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/lock")
    public LockMicResponse lockMic(@PathVariable("roomId") String roomId,
            @RequestBody LockMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        LockMicResponse response = new LockMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        if (!roomInfo.getOwner().getUid().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can lock mic");
        }
        this.voiceRoomMicService.lockMic(roomInfo.getChatroomId(), request.getIndex());

        return response;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/lock")
    public UnLockMicResponse unLockMic(@PathVariable("roomId") String roomId,
            @RequestBody UnLockMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        UnLockMicResponse response = new UnLockMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        if (!roomInfo.getOwner().getUid().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can unlock mic");
        }
        this.voiceRoomMicService.unLockMic(roomInfo.getChatroomId(), request.getIndex());
        return response;
    }

    //群主邀请上麦
    @PostMapping("/voice/room/{roomId}/mic/invite")
    public InviteUserOnMicResponse invite(@PathVariable("roomId") String roomId,
            @RequestBody InviteUserOnMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        InviteUserOnMicResponse response = new InviteUserOnMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        if (!roomInfo.getOwner().getUid().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can invite");
        }
        this.voiceRoomMicService.invite(roomInfo, request.getIndex(), request.getUid());
        return response;
    }

    //群主同意上麦申请
    @PostMapping("/voice/room/{roomId}/mic/apply/agree")
    public ApplyAgreeOnMicResponse agreeApply(
            @PathVariable("roomId") String roomId,
            @RequestBody ApplyAgreeOnMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        ApplyAgreeOnMicResponse response = new ApplyAgreeOnMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        if (!roomInfo.getOwner().getUid().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can invite");
        }
        Boolean result = micApplyUserService.agreeApply(roomInfo, request.getUid());
        response.setResult(result);
        return response;
    }

    //用户同意邀请上麦申请
    @GetMapping("/voice/room/{roomId}/mic/invite/agree")
    public InviteAgreeOnMicResponse agreeInvite(
            @PathVariable("roomId") String roomId,
            @RequestBody InviteAgreeOnMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("user must not be null");
        }
        InviteAgreeOnMicResponse response = new InviteAgreeOnMicResponse(Boolean.TRUE);
        VoiceRoomDTO roomInfo = voiceRoomService.getByRoomId(roomId);
        if (roomInfo == null) {
            throw new IllegalArgumentException("room is not be found!");
        }
        Boolean result =
                voiceRoomMicService.agreeInvite(roomInfo.getChatroomId(), request.getUid(),
                        request.getIndex());
        response.setResult(result);
        return response;
    }

}
