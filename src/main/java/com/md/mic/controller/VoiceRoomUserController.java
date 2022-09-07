package com.md.mic.controller;

import com.md.mic.exception.UserNotFoundException;
import com.md.mic.exception.VoiceRoomSecurityException;
import com.md.mic.pojos.*;
import com.md.mic.service.VoiceRoomMicService;
import com.md.mic.service.VoiceRoomService;
import com.md.mic.service.VoiceRoomUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
public class VoiceRoomUserController {

    @Resource
    private VoiceRoomUserService voiceRoomUserService;

    @Resource
    private VoiceRoomService voiceRoomService;

    @Resource
    private VoiceRoomMicService voiceRoomMicService;

    @GetMapping("/voice/room/{roomId}/members/list")
    public GetRoomUserListResponse getRoomMemberList(@PathVariable("roomId") String roomId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit) {
        if (limit > 100) {
            throw new IllegalArgumentException("exceeded maximum paging limit");
        }
        PageInfo<UserDTO> pageInfo =
                voiceRoomUserService.findPageByRoomId(roomId, cursor, limit);
        if (pageInfo.getList() == null || pageInfo.getList().isEmpty()) {
            return new GetRoomUserListResponse(0L, null, Collections.emptyList());
        }
        return new GetRoomUserListResponse(pageInfo.getTotal(), pageInfo.getCursor(),
                pageInfo.getList());
    }

    @PostMapping("/voice/room/{roomId}/members/join")
    public JoinRoomResponse joinRoom(@PathVariable("roomId") String roomId,
            @RequestBody JoinRoomRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("join room user must not be null");
        }
        voiceRoomUserService.addVoiceRoomUser(roomId, user.getUid(), request.getPassword());
        VoiceRoomDTO voiceRoomDTO = voiceRoomService.getByRoomId(roomId);
        List<MicInfo> micInfo = voiceRoomMicService.getByRoomId(roomId);
        return new JoinRoomResponse(voiceRoomDTO, micInfo);
    }

    @DeleteMapping("/voice/room/{roomId}/members/leave")
    public LeaveRoomResponse leaveRoom(@PathVariable("roomId") String roomId,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException("leave room user must not be null");
        }
        voiceRoomUserService.deleteVoiceRoomUser(roomId, user.getUid());
        return new LeaveRoomResponse(Boolean.TRUE);
    }

    @DeleteMapping("/voice/room/{roomId}/members/kick")
    public KickRoomResponse kickRoom(@PathVariable("roomId") String roomId,
            @RequestBody KickRoomRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new VoiceRoomSecurityException("not the owner can't operate");
        }
        voiceRoomUserService.kickVoiceRoomUser(roomId, user.getUid(), request.getUid());
        return new KickRoomResponse(Boolean.TRUE);
    }

}
