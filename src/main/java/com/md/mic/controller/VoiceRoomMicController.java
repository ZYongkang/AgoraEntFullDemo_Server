package com.md.mic.controller;

import com.md.common.util.ValidationUtil;
import com.md.mic.exception.RoomNotFoundException;
import com.md.mic.exception.UserNotFoundException;
import com.md.mic.exception.UserNotInRoomException;
import com.md.mic.exception.VoiceRoomSecurityException;
import com.md.mic.model.VoiceRoom;
import com.md.mic.model.VoiceRoomUser;
import com.md.mic.pojos.*;
import com.md.mic.pojos.vo.MicApplyVO;
import com.md.mic.service.MicApplyUserService;
import com.md.mic.service.VoiceRoomMicService;
import com.md.mic.service.VoiceRoomService;
import com.md.mic.service.VoiceRoomUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
public class VoiceRoomMicController {

    @Resource
    private MicApplyUserService micApplyUserService;

    @Resource
    private VoiceRoomMicService voiceRoomMicService;

    @Resource
    private VoiceRoomService voiceRoomService;

    @Resource
    private VoiceRoomUserService voiceRoomUserService;

    @GetMapping("/voice/room/{roomId}/mic/apply")
    public GetMicApplyListResponse getMicApplyList(
            @RequestAttribute(name = "user", required = false) UserDTO user,
            @PathVariable("roomId") String roomId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit) {
        validateMicPermissions(roomId, user.getUid());
        PageInfo<MicApplyVO> pageInfo = micApplyUserService.getByPage(roomId, cursor, limit);
        return new GetMicApplyListResponse(pageInfo.getTotal(), pageInfo.getCursor(),
                pageInfo.getList());
    }

    @PostMapping("/voice/room/{roomId}/mic/apply")
    public AddMicApplyResponse addMicApply(@PathVariable("roomId") String roomId,
            @RequestBody @Validated AddMicApplyRequest request, BindingResult bindingResult,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        ValidationUtil.validate(bindingResult);
        if (user == null) {
            throw new UserNotFoundException();
        }
        VoiceRoom roomInfo = validateMicPermissions(roomId, user.getUid());
        if (user.getUid().equals(roomInfo.getOwner())) {
            throw new VoiceRoomSecurityException("admin can not apply mic");
        }
        Boolean result =
                micApplyUserService.addMicApply(user.getUid(), roomInfo,
                        request == null ? null : request.getMicIndex());
        return new AddMicApplyResponse(result);
    }

    @DeleteMapping("/voice/room/{roomId}/mic/apply")
    public DeleteMicApplyResponse deleteMicApply(@PathVariable("roomId") String roomId,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        validateMicPermissions(roomId, user.getUid());
        micApplyUserService.deleteMicApply(user.getUid(), roomId);
        return new DeleteMicApplyResponse(Boolean.TRUE);
    }

    @GetMapping("/voice/room/{roomId}/mic")
    public List<MicInfo> getRoomMicInfo(
            @RequestAttribute(name = "user", required = false) UserDTO user,
            @PathVariable("roomId") String roomId) {
        VoiceRoom roomInfo = validateMicPermissions(roomId, user.getUid());
        return voiceRoomMicService.getRoomMicInfo(roomInfo);
    }

    //闭麦 todo 不要用这种注释
    @PostMapping("/voice/room/{roomId}/mic/close")
    public CloseMicResponse closeMic(@PathVariable("roomId") String roomId,
            @RequestBody @Validated CloseMicRequest request, BindingResult bindingResult,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        ValidationUtil.validate(bindingResult);
        if (user == null) {
            throw new UserNotFoundException();
        }
        VoiceRoom voiceRoom = validateMicPermissions(roomId, user.getUid());
        this.voiceRoomMicService.closeMic(user.getUid(), voiceRoom.getChatroomId(),
                request.getMicIndex());
        return new CloseMicResponse(Boolean.TRUE);
    }

    //取消关麦、开麦
    @DeleteMapping("/voice/room/{roomId}/mic/close")
    public OpenMicResponse openMic(@PathVariable("roomId") String roomId,
            @RequestParam("mic_index") Integer micIndex,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        OpenMicResponse response = new OpenMicResponse(Boolean.TRUE);
        VoiceRoom roomInfo = validateMicPermissions(roomId, user.getUid());
        this.voiceRoomMicService.openMic(user.getUid(), roomInfo.getChatroomId(),
                micIndex);
        return response;
    }

    //下麦
    @DeleteMapping("/voice/room/{roomId}/mic/leave")
    public LeaveMicResponse leaveMic(@PathVariable("roomId") String roomId,
            @RequestParam("mic_index") Integer micIndex,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        LeaveMicResponse response = new LeaveMicResponse(Boolean.TRUE);
        VoiceRoom roomInfo = validateMicPermissions(roomId, user.getUid());
        this.voiceRoomMicService.leaveMic(user.getUid(), roomInfo.getChatroomId(),
                micIndex);
        return response;
    }

    //禁言麦位
    @PostMapping("/voice/room/{roomId}/mic/mute")
    public MuteMicResponse muteMic(@PathVariable("roomId") String roomId,
            @RequestBody @Validated MuteMicRequest request, BindingResult bindingResult,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        ValidationUtil.validate(bindingResult);
        if (user == null) {
            throw new UserNotFoundException();
        }
        MuteMicResponse response = new MuteMicResponse(Boolean.TRUE);
        VoiceRoom roomInfo = voiceRoomService.findByRoomId(roomId);
        if (!roomInfo.getOwner().equals(user.getUid())) {
            throw new VoiceRoomSecurityException("only the owner can operate");
        }
        this.voiceRoomMicService.muteMic(roomInfo.getChatroomId(), request.getMicIndex());

        return response;
    }

    //取消禁言
    @DeleteMapping("/voice/room/{roomId}/mic/mute")
    public UnMuteMicResponse unMuteMic(@PathVariable("roomId") String roomId,
            @RequestParam("mic_index") Integer micIndex,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        UnMuteMicResponse response = new UnMuteMicResponse(Boolean.TRUE);
        VoiceRoom roomInfo = voiceRoomService.findByRoomId(roomId);
        if (!roomInfo.getOwner().equals(user.getUid())) {
            throw new VoiceRoomSecurityException("only the owner can operate");
        }
        this.voiceRoomMicService.unMuteMic(roomInfo.getChatroomId(), micIndex);

        return response;
    }

    //交换麦位
    @PostMapping("/voice/room/{roomId}/mic/exchange")
    public ExchangeMicResponse exchangeMic(@PathVariable("roomId") String roomId,
            @RequestBody @Validated ExchangeMicRequest request, BindingResult bindingResult,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        ValidationUtil.validate(bindingResult);
        if (user == null) {
            throw new UserNotFoundException();
        }
        ExchangeMicResponse response = new ExchangeMicResponse(Boolean.TRUE);
        VoiceRoom roomInfo = validateMicPermissions(roomId, user.getUid());
        this.voiceRoomMicService.exchangeMic(roomInfo.getChatroomId(),
                request.getFrom(), request.getTo(), user.getUid());
        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/kick")
    public KickUserMicResponse kickUserMic(
            @PathVariable("roomId") String roomId,
            @RequestBody @Validated KickUserMicRequest request, BindingResult bindingResult,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        ValidationUtil.validate(bindingResult);
        if (user == null) {
            throw new UserNotFoundException();
        }
        KickUserMicResponse response = new KickUserMicResponse(Boolean.TRUE);
        VoiceRoom roomInfo = voiceRoomService.findByRoomId(roomId);
        if (!roomInfo.getOwner().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can kick mic");
        }
        this.voiceRoomMicService.kickUserMic(roomInfo.getChatroomId(), request.getMicIndex(),
                request.getUid());

        return response;
    }

    @PostMapping("/voice/room/{roomId}/mic/lock")
    public LockMicResponse lockMic(@PathVariable("roomId") String roomId,
            @RequestBody @Validated LockMicRequest request, BindingResult bindingResult,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        ValidationUtil.validate(bindingResult);
        if (user == null) {
            throw new UserNotFoundException();
        }
        LockMicResponse response = new LockMicResponse(Boolean.TRUE);
        VoiceRoom roomInfo = voiceRoomService.findByRoomId(roomId);
        if (!roomInfo.getOwner().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can lock mic");
        }
        this.voiceRoomMicService.lockMic(roomInfo.getChatroomId(), request.getMicIndex());
        return response;
    }

    @DeleteMapping("/voice/room/{roomId}/mic/lock")
    public UnLockMicResponse unLockMic(@PathVariable("roomId") String roomId,
            @RequestParam("mic_index") Integer micIndex,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        UnLockMicResponse response = new UnLockMicResponse(Boolean.TRUE);
        VoiceRoom roomInfo = voiceRoomService.findByRoomId(roomId);
        if (roomInfo == null) {
            throw new RoomNotFoundException(String.format("room %s not found", roomId));
        }
        if (!roomInfo.getOwner().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can unlock mic");
        }
        this.voiceRoomMicService.unLockMic(roomInfo.getChatroomId(), micIndex);
        return response;
    }

    //群主邀请上麦
    @PostMapping("/voice/room/{roomId}/mic/invite")
    public InviteUserOnMicResponse invite(@PathVariable("roomId") String roomId,
            @RequestBody InviteUserOnMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        InviteUserOnMicResponse response = new InviteUserOnMicResponse(Boolean.TRUE);
        VoiceRoom roomInfo = voiceRoomService.findByRoomId(roomId);
        if (roomInfo == null) {
            throw new RoomNotFoundException(String.format("room %s not found", roomId));
        }
        if (!roomInfo.getOwner().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can invite");
        }
        validateMicPermissions(roomId, request.getUid());
        this.voiceRoomMicService.invite(roomInfo, request.getMicIndex(), request.getUid());
        return response;
    }

    //群主同意上麦申请
    @PostMapping("/voice/room/{roomId}/mic/apply/agree")
    public ApplyAgreeOnMicResponse agreeApply(
            @PathVariable("roomId") String roomId,
            @RequestBody ApplyAgreeOnMicRequest request, BindingResult bindingResult,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        ValidationUtil.validate(bindingResult);
        if (user == null) {
            throw new UserNotFoundException();
        }

        VoiceRoom roomInfo = voiceRoomService.findByRoomId(roomId);
        if (roomInfo == null) {
            throw new RoomNotFoundException(String.format("room %s not found", roomId));
        }
        if (!roomInfo.getOwner().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can invite");
        }
        Boolean result = micApplyUserService.agreeApply(roomInfo, request.getUid());
        return new ApplyAgreeOnMicResponse(Boolean.TRUE.equals(result));
    }

    //群主拒绝上麦申请
    @PostMapping("/voice/room/{roomId}/mic/apply/refuse")
    public ApplyAgreeOnMicResponse refuseApply(
            @PathVariable("roomId") String roomId,
            @RequestBody ApplyRefuseOnMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        VoiceRoom roomInfo = voiceRoomService.findByRoomId(roomId);
        if (roomInfo == null) {
            throw new RoomNotFoundException(String.format("room %s not found", roomId));
        }
        if (!roomInfo.getOwner().equals(user.getUid())) {
            throw new IllegalArgumentException("only the admin can invite");
        }
        Boolean result =
                micApplyUserService.refuseApply(roomInfo, request.getUid(), request.getMicIndex());
        return new ApplyAgreeOnMicResponse(Boolean.TRUE.equals(result));
    }

    //用户同意邀请上麦申请
    @PostMapping("/voice/room/{roomId}/mic/invite/agree")
    public InviteAgreeOnMicResponse agreeInvite(
            @PathVariable("roomId") String roomId,
            @RequestBody InviteAgreeOnMicRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        VoiceRoom roomInfo = voiceRoomService.findByRoomId(roomId);
        if (roomInfo == null) {
            throw new RoomNotFoundException(String.format("room %s not found", roomId));
        }
        if (!user.getUid().equals(request.getUid())) {
            throw new VoiceRoomSecurityException("agree user is not the operator");
        }
        Boolean result =
                voiceRoomMicService.agreeInvite(roomInfo, user.getUid(),
                        request.getMicIndex());
        return new InviteAgreeOnMicResponse(Boolean.TRUE.equals(result));
    }

    //用户拒绝上麦邀请
    @GetMapping("/voice/room/{roomId}/mic/invite/refuse")
    public InviteAgreeOnMicResponse refuseInvite(
            @PathVariable("roomId") String roomId,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        VoiceRoom roomInfo = voiceRoomService.findByRoomId(roomId);
        if (roomInfo == null) {
            throw new RoomNotFoundException(String.format("room %s not found", roomId));
        }
        Boolean result =
                voiceRoomMicService.refuseInvite(roomInfo, user.getUid());
        return new InviteAgreeOnMicResponse(Boolean.TRUE.equals(result));
    }

    private VoiceRoom validateMicPermissions(String roomId, String uid) {
        VoiceRoom roomInfo = voiceRoomService.findByRoomId(roomId);
        VoiceRoomUser voiceRoomUser =
                voiceRoomUserService.findByRoomIdAndUid(roomInfo.getRoomId(), uid);
        if (!uid.equals(roomInfo.getOwner()) && voiceRoomUser == null) {
            throw new UserNotInRoomException();
        }
        return roomInfo;
    }

}
