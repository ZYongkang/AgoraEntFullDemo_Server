package com.md.mic.controller;

import com.md.common.util.ValidationUtil;
import com.md.common.util.token.TokenProvider;
import com.md.mic.common.jwt.util.VoiceRoomJwtUtil;
import com.md.mic.pojos.LoginRequest;
import com.md.mic.pojos.LoginResponse;
import com.md.mic.pojos.UserDTO;
import com.md.mic.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private VoiceRoomJwtUtil voiceRoomJwtUtil;

    @Resource
    private TokenProvider tokenProvider;

    @PostMapping(value = "/login/device")
    public LoginResponse loginDevice(@RequestBody @Validated LoginRequest request,
            BindingResult result) {
        ValidationUtil.validate(result);
        UserDTO userDTO = userService.loginDevice(request.getDeviceId(), request.getName(),
                request.getPortrait());
        String jwtToken = voiceRoomJwtUtil.createJWT(userDTO.getUid());
        String imToken = tokenProvider.buildImToken(userDTO.getChatUuid());
        return new LoginResponse(userDTO.getUid(), userDTO.getName(), userDTO.getPortrait(),
                userDTO.getChatUid(), jwtToken, imToken, userDTO.getRtcUid());
    }
}
