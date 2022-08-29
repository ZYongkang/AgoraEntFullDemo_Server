package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.common.im.ImApi;
import com.md.mic.model.EasemobUser;
import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.CreateRoomRequest;
import com.md.mic.pojos.UserDTO;
import com.md.mic.pojos.VoiceRoomDTO;
import com.md.mic.repository.VoiceRoomMapper;
import com.md.mic.service.EasemobUserService;
import com.md.mic.service.VoiceRoomService;
import com.md.service.model.entity.Users;
import com.md.service.service.RoomInfoService;
import com.md.service.service.RoomUsersService;
import com.md.service.service.UsersService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

@Slf4j
@Service
public class VoiceRoomServiceImpl extends ServiceImpl<VoiceRoomMapper, VoiceRoom>
        implements VoiceRoomService {

    @Resource
    private RoomInfoService roomInfoService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UsersService usersService;

    @Resource
    private RoomUsersService roomUsersService;

    @Resource
    private EasemobUserService easemobUserService;

    @Resource
    private ImApi imApi;

    @Override
    @Transactional
    public VoiceRoomDTO create(Users user, CreateRoomRequest request) {
        String userNo = user.getUserNo();
        LambdaQueryWrapper<EasemobUser> queryWrapper =
                new LambdaQueryWrapper<EasemobUser>().eq(EasemobUser::getUid, userNo);
        String userChatId;
        VoiceRoom voiceRoom;
        try {
            EasemobUser easemobUser = easemobUserService.getOne(queryWrapper);
            userChatId = easemobUser.getChatId();
            String chatRoomId = imApi.createChatRoom(request.getName(), userChatId,
                    Collections.singletonList(userChatId), request.getName());
            voiceRoom = VoiceRoom.create(request.getName(), chatRoomId, request.getIsPrivate(),
                    request.getPassword(), request.getAllowFreeJoinMic(),
                    request.getType(), userChatId, request.getBgUrl(),
                    request.getSoundEffect(), request.getAnnouncement());
            baseMapper.insert(voiceRoom);
        } catch (Exception e) {
            log.error("create room failed | err=", e);
            throw e;
        }
        UserDTO owner =
                new UserDTO(user.getUserNo(), userChatId, user.getName(), user.getHeadUrl());
        return VoiceRoomDTO.from(voiceRoom, owner);
    }
}
