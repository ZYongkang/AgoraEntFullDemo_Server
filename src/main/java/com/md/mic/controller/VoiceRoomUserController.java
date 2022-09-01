package com.md.mic.controller;

import com.md.mic.exception.UserNotFoundException;
import com.md.mic.model.User;
import com.md.mic.pojos.*;
import com.md.mic.service.UserService;
import com.md.mic.service.VoiceRoomService;
import com.md.mic.service.VoiceRoomUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;

@Slf4j
@RestController
public class VoiceRoomUserController {

    @Resource
    private VoiceRoomUserService voiceRoomUserService;

    @Resource
    private VoiceRoomService voiceRoomService;

    @GetMapping("/voice/room/{roomId}/members/list")
    public GetRoomUserListResponse getRoomMemberList(@PathVariable("roomId") String roomId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit) {
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
            @RequestAttribute("user") User user) {
        if (user == null) {
            throw new UserNotFoundException("join room user must not be null");
        }
        voiceRoomUserService.addVoiceRoomUser(roomId, user, request.getPassword());
        VoiceRoomDTO voiceRoomDTO = voiceRoomService.getByRoomId(roomId);
        return new JoinRoomResponse(voiceRoomDTO);
    }

    @DeleteMapping("/voice/room/{roomId}/members/leave")
    public LeaveRoomResponse leaveRoom(@PathVariable("roomId") String roomId,
            @RequestAttribute("user") User user) {
        if (user == null) {
            throw new UserNotFoundException("join room user must not be null");
        }
        return new LeaveRoomResponse(Boolean.TRUE);
    }

    @DeleteMapping("/voice/room/{roomId}/members/kick")
    public KickRoomResponse kickRoom(@PathVariable("roomId") String roomId,
            @RequestBody KickRoomRequest request,
            @RequestAttribute("user") User user) {
        if (user == null) {
            throw new UserNotFoundException("join room user must not be null");
        }
        return new KickRoomResponse(Boolean.TRUE);
    }

}
