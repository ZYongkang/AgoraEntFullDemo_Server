package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.common.im.ImApi;
import com.md.mic.exception.RoomNotFoundException;
import com.md.mic.model.EasemobUser;
import com.md.mic.model.User;
import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.*;
import com.md.mic.repository.VoiceRoomMapper;
import com.md.mic.service.EasemobUserService;
import com.md.mic.service.UserService;
import com.md.mic.service.VoiceRoomService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VoiceRoomServiceImpl extends ServiceImpl<VoiceRoomMapper, VoiceRoom>
        implements VoiceRoomService {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private EasemobUserService easemobUserService;

    @Resource
    private UserService userService;

    @Resource
    private ImApi imApi;

    @Resource
    private RedisTemplate redisTemplate;

    @Value("${local.zone.offset:+8}")
    private String zoneOffset;

    @Override
    @Transactional
    public VoiceRoomDTO create(User user, CreateRoomRequest request) {
        String uid = user.getUid();
        LambdaQueryWrapper<EasemobUser> queryWrapper =
                new LambdaQueryWrapper<EasemobUser>().eq(EasemobUser::getUid, uid);
        String userChatId;
        VoiceRoom voiceRoom;
        try {
            EasemobUser easemobUser = easemobUserService.getOne(queryWrapper);
            userChatId = easemobUser.getChatId();
            String chatRoomId = imApi.createChatRoom(request.getName(), userChatId,
                    Collections.singletonList(userChatId), request.getName());
            voiceRoom = VoiceRoom.create(request.getName(), chatRoomId, request.getIsPrivate(),
                    request.getPassword(), request.getAllowFreeJoinMic(),
                    request.getType(), uid, request.getSoundEffect());
            try {
                save(voiceRoom);
            } catch (Exception e) {
                log.error("save voice room failed | room={}, err=", voiceRoom, e);
                // todo imAPi.deleteChatRoom(chatRoomId);
                throw e;
            }
            UserDTO owner = UserDTO.builder().uid(user.getUid())
                    .chatUuid(easemobUser.getChatUuid())
                    .chatUid(userChatId)
                    .name(user.getName())
                    .portrait(user.getPortrait())
                    .build();
            Long clickCount = getClickCount(voiceRoom.getRoomId());
            Long memberCount = getMemberCount(voiceRoom.getRoomId());
            return VoiceRoomDTO.from(voiceRoom, owner, memberCount, clickCount);
        } catch (Exception e) {
            log.error("create room failed | err=", e);
            throw e;
        }
    }

    @Override
    public PageInfo<RoomListDTO> getByPage(String cursor, int limit) {
        List<VoiceRoom> voiceRoomList;
        int limitSize = limit + 1;
        Long total = baseMapper.selectCount(new LambdaQueryWrapper<>());
        if (StringUtils.isBlank(cursor)) {
            LambdaQueryWrapper<VoiceRoom> queryWrapper =
                    new LambdaQueryWrapper<VoiceRoom>().orderByDesc(VoiceRoom::getId)
                            .last(" limit " + limitSize);
            voiceRoomList = baseMapper.selectList(queryWrapper);
        } else {
            String s = new String(
                    Base64.getUrlDecoder().decode(cursor.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
            int id = Integer.parseInt(s);
            LambdaQueryWrapper<VoiceRoom> queryWrapper =
                    new LambdaQueryWrapper<VoiceRoom>().le(VoiceRoom::getId, id)
                            .orderByDesc(VoiceRoom::getId)
                            .last(" limit " + limitSize);
            voiceRoomList = baseMapper.selectList(queryWrapper);
        }
        if (voiceRoomList.size() == limitSize) {
            VoiceRoom voiceRoom = voiceRoomList.get(limitSize - 1);
            Integer id = voiceRoom.getId();
            cursor = Base64.getUrlEncoder()
                    .encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
            voiceRoomList.remove(voiceRoom);
        }
        List<String> ownerUidList =
                voiceRoomList.stream().map(VoiceRoom::getOwner).collect(Collectors.toList());
        Map<String, UserDTO> ownerMap = userService.findByUidList(ownerUidList);
        List<RoomListDTO> list = voiceRoomList.stream().map(voiceRoom -> {
            UserDTO userDTO = ownerMap.get(voiceRoom.getOwner());
            long createdAt = voiceRoom.getCreatedAt().toInstant(ZoneOffset.of(zoneOffset))
                    .toEpochMilli();
            Long memberCount = getMemberCount(voiceRoom.getRoomId());
            return new RoomListDTO(voiceRoom.getRoomId(), voiceRoom.getChannelId(),
                    voiceRoom.getChatroomId(),
                    voiceRoom.getName(), userDTO, voiceRoom.getIsPrivate(),
                    voiceRoom.getType(), createdAt, memberCount);
        }).collect(Collectors.toList());
        PageInfo<RoomListDTO> pageInfo = new PageInfo<>();
        pageInfo.setCursor(cursor);
        pageInfo.setTotal(total);
        pageInfo.setList(list);
        return pageInfo;
    }

    @Override public VoiceRoomDTO getByRoomId(String roomId) {
        LambdaQueryWrapper<VoiceRoom> queryWrapper =
                new LambdaQueryWrapper<VoiceRoom>().eq(VoiceRoom::getRoomId, roomId);
        VoiceRoom voiceRoom = baseMapper.selectOne(queryWrapper);
        if (voiceRoom == null) {
            throw new RoomNotFoundException(String.format("room %s not found", roomId));
        }

        UserDTO userDTO = userService.getByUid(voiceRoom.getOwner());
        long createdAt = voiceRoom.getCreatedAt().toInstant(ZoneOffset.of(zoneOffset))
                .toEpochMilli();
        Long memberCount = getMemberCount(voiceRoom.getRoomId());
        new RoomListDTO(voiceRoom.getRoomId(), voiceRoom.getChannelId(),
                voiceRoom.getChatroomId(),
                voiceRoom.getName(), userDTO, voiceRoom.getIsPrivate(),
                voiceRoom.getType(), createdAt, memberCount);
        return null;
    }

    private Long getClickCount(String roomId) {
        String key = String.format("room:voice:%s:memberCount", roomId);
        try {
            return redisTemplate.opsForValue().increment(key, 0L);
        } catch (Exception e) {
            log.error("get room click count failed | roomId={}, err=", roomId, e);
            return 0L;
        }
    }

    private Long getMemberCount(String roomId) {
        String key = String.format("room:voice:%s:clickCount", roomId);
        try {
            return redisTemplate.opsForValue().increment(key, 0L);
        } catch (Exception e) {
            log.error("get room member count failed | roomId={}, err=", roomId, e);
            return 0L;
        }
    }

    private Long incrClickCount(String roomId) {
        String key = String.format("room:voice:%s:memberCount", roomId);
        return redisTemplate.opsForValue().increment(key);
    }

    private Long incrMemberCount(String roomId) {
        String key = String.format("room:voice:%s:clickCount", roomId);
        return redisTemplate.opsForValue().increment(key);
    }

}
