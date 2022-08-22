package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.md.common.im.ImApi;
import com.md.common.util.EncryptionUtil;
import com.md.mic.exception.RoomNotFoundException;
import com.md.mic.exception.VoiceRoomSecurityException;
import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.*;
import com.md.mic.repository.VoiceRoomMapper;
import com.md.mic.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private UserService userService;

    @Resource
    private VoiceRoomUserService voiceRoomUserService;

    @Resource
    private VoiceRoomMicService voiceRoomMicService;

    @Resource
    private ImApi imApi;

    @Resource(name = "voiceRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private EncryptionUtil encryptionUtil;

    @Value("${voice.room.redis.cache.ttl:PT1H}")
    private Duration ttl;

    @Value("${local.zone.offset:+8}")
    private String zoneOffset;

    @Value("${voice.room.mic.count.default:6}")
    private Integer micCount;

    @Value("${voice.room.robot.count.default:2}")
    private Integer robotCount;


    @Override
    @Transactional
    public Tuple2<VoiceRoomDTO, List<MicInfo>> create(UserDTO owner, CreateRoomRequest request) {
        String uid = owner.getUid();
        VoiceRoom voiceRoom;
        String userChatId = owner.getChatUid();
        String chatRoomId = imApi.createChatRoom(request.getName(), userChatId,
                Collections.singletonList(userChatId), request.getName());
        String password = request.getPassword();
        if (Boolean.TRUE.equals(request.getIsPrivate())) {
            password = encryptionUtil.getEncryptedPwd(password);
        }
        voiceRoom = VoiceRoom.create(request.getName(), chatRoomId, request.getIsPrivate(),
                password, request.getAllowFreeJoinMic(),
                request.getType(), uid, request.getSoundEffect(), false, micCount, robotCount);
        List<MicInfo> micInfos = voiceRoomMicService.initMic(voiceRoom, voiceRoom.getUseRobot());
        try {
            save(voiceRoom);
        } catch (Exception e) {
            log.error("save voice room failed | room={}, err=", voiceRoom, e);
            imApi.deleteChatRoom(voiceRoom.getChatroomId());
            throw e;
        }
        Long clickCount = 0L;
        Long memberCount = 0L;
        Long giftAmount = 0L;
        VoiceRoomDTO roomDTO =
                VoiceRoomDTO.from(voiceRoom, owner, memberCount, clickCount, giftAmount);
        return Tuples.of(roomDTO, micInfos);
    }

    @Override
    public PageInfo<RoomListDTO> getByPage(String cursor, int limit, Integer type) {
        List<VoiceRoom> voiceRoomList;
        int limitSize = limit + 1;
        LambdaQueryWrapper<VoiceRoom> totalQueryWrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            totalQueryWrapper.eq(VoiceRoom::getType, type);
        }
        Long total = baseMapper.selectCount(totalQueryWrapper);
        if (StringUtils.isBlank(cursor)) {
            LambdaQueryWrapper<VoiceRoom> queryWrapper =
                    new LambdaQueryWrapper<>();
            if (type != null) {
                queryWrapper.eq(VoiceRoom::getType, type);
            }
            queryWrapper.orderByDesc(VoiceRoom::getId)
                    .last(" limit " + limitSize);
            voiceRoomList = baseMapper.selectList(queryWrapper);
        } else {
            String s = new String(
                    Base64.getUrlDecoder().decode(cursor.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
            int id = Integer.parseInt(s);
            LambdaQueryWrapper<VoiceRoom> queryWrapper =
                    new LambdaQueryWrapper<>();
            if (type != null) {
                queryWrapper.eq(VoiceRoom::getType, type);
            }
            queryWrapper.le(VoiceRoom::getId, id)
                    .orderByDesc(VoiceRoom::getId)
                    .last(" limit " + limitSize);
            voiceRoomList = baseMapper.selectList(queryWrapper);
        }

        if (voiceRoomList == null || voiceRoomList.isEmpty()) {
            PageInfo<RoomListDTO> pageInfo = new PageInfo<>();
            pageInfo.setCursor(null);
            pageInfo.setTotal(0L);
            pageInfo.setList(Collections.emptyList());
            return pageInfo;
        }

        if (voiceRoomList.size() == limitSize) {
            VoiceRoom voiceRoom = voiceRoomList.get(limitSize - 1);
            Integer id = voiceRoom.getId();
            cursor = Base64.getUrlEncoder()
                    .encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
            voiceRoomList.remove(voiceRoom);
        } else {
            cursor = null;
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

    @Override
    @Transactional
    public void updateByRoomId(String roomId, UpdateRoomInfoRequest request, String owner) {
        VoiceRoom voiceRoom = findByRoomId(roomId);
        if (!owner.equals(voiceRoom.getOwner())) {
            throw new VoiceRoomSecurityException("not the owner can't operate");
        }
        if (StringUtils.isNotBlank(request.getName())) {
            voiceRoom = voiceRoom.updateName(request.getName());
        }
        if (request.getIsPrivate() != null) {
            voiceRoom = voiceRoom.updateIsPrivate(request.getIsPrivate());
        }
        if (StringUtils.isNotBlank(request.getPassword())) {
            voiceRoom = voiceRoom.updatePassword(request.getPassword());
        }
        if (request.getAllowedFreeJoinMic() != null) {
            voiceRoom = voiceRoom.updateAllowedFreeJoinMic(request.getAllowedFreeJoinMic());
        }
        if (StringUtils.isNotBlank(request.getAnnouncement())) {
            voiceRoom = voiceRoom.updateAnnouncement(request.getAnnouncement());
            imApi.setAnnouncement(voiceRoom.getChatroomId(), request.getAnnouncement());
        }
        if (request.getUseRobot() != null) {
            voiceRoom = voiceRoom.updateUseRobot(request.getUseRobot());
            voiceRoomMicService.updateRobotMicStatus(voiceRoom, request.getUseRobot());
        }
        updateById(voiceRoom);
        Boolean hasKey = redisTemplate.hasKey(key(roomId));
        if (Boolean.TRUE.equals(hasKey)) {
            redisTemplate.delete(key(roomId));
        }
    }

    @Override
    @Transactional
    public void deleteByRoomId(String roomId, String owner) {
        VoiceRoom voiceRoom = findByRoomId(roomId);
        if (!owner.equals(voiceRoom.getOwner())) {
            throw new VoiceRoomSecurityException("not the owner can't operate");
        }
        imApi.deleteChatRoom(voiceRoom.getChatroomId());
        voiceRoomUserService.deleteByRoomId(roomId);
        LambdaQueryWrapper<VoiceRoom> queryWrapper =
                new LambdaQueryWrapper<VoiceRoom>().eq(VoiceRoom::getRoomId, roomId);
        baseMapper.delete(queryWrapper);
        Boolean hasKey = redisTemplate.hasKey(key(roomId));
        if (Boolean.TRUE.equals(hasKey)) {
            redisTemplate.delete(key(roomId));
        }
    }

    public Long getClickCount(String roomId) {
        String key = String.format("room:voice:%s:memberCount", roomId);
        try {
            return redisTemplate.opsForValue().increment(key, 0L);
        } catch (Exception e) {
            log.error("get room click count failed | roomId={}, err=", roomId, e);
            return 0L;
        }
    }

    public Long getMemberCount(String roomId) {
        String key = String.format("room:voice:%s:clickCount", roomId);
        try {
            return redisTemplate.opsForValue().increment(key, 0L);
        } catch (Exception e) {
            log.error("get room member count failed | roomId={}, err=", roomId, e);
            return 0L;
        }
    }

    public VoiceRoom findByRoomId(String roomId) {
        VoiceRoom voiceRoom = null;
        Boolean hasKey = redisTemplate.hasKey(key(roomId));
        if (Boolean.TRUE.equals(hasKey)) {
            String json = redisTemplate.opsForValue().get(key(roomId));
            try {
                voiceRoom = objectMapper.readValue(json, VoiceRoom.class);
            } catch (JsonProcessingException e) {
                log.error("parse voice room json cache failed | roomId={},"
                        + " json={}, e=", json, e);
            }
        }
        if (voiceRoom == null) {
            LambdaQueryWrapper<VoiceRoom> queryWrapper =
                    new LambdaQueryWrapper<VoiceRoom>().eq(VoiceRoom::getRoomId, roomId);
            voiceRoom = baseMapper.selectOne(queryWrapper);
            if (voiceRoom != null) {
                try {
                    String json = objectMapper.writeValueAsString(voiceRoom);
                    redisTemplate.opsForValue().set(key(roomId), json, ttl);
                } catch (JsonProcessingException e) {
                    log.error("write voice room json cache failed | roomId={},"
                            + " voiceRoom={}, e=", voiceRoom, e);
                }
            }
        }
        if (voiceRoom == null) {
            throw new RoomNotFoundException(String.format("room %s not found", roomId));
        }
        return voiceRoom;
    }

    private String key(String roomId) {
        return String.format("voiceRoom:roomId:%s", roomId);
    }
}
