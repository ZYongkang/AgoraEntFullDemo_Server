package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.md.common.im.ImApi;
import com.md.mic.exception.RoomNotFoundException;
import com.md.mic.exception.VoiceRoomSecurityException;
import com.md.mic.model.GiftRecord;
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

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.*;
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
    private GiftRecordService giftRecordService;

    @Resource
    private VoiceRoomMicService voiceRoomMicService;

    @Resource
    private ImApi imApi;

    @Resource(name = "voiceRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Value("${voice.room.redis.cache.ttl:PT1H}")
    private Duration ttl;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${local.zone.offset:+8}")
    private String zoneOffset;

    @Value("${ranking.length:100}")
    private Integer rankingLength;

    @Override
    @Transactional
    public VoiceRoomDTO create(UserDTO owner, CreateRoomRequest request) {
        String uid = owner.getUid();
        String userChatId;
        VoiceRoom voiceRoom;
        try {
            userChatId = owner.getChatUid();
            String chatRoomId = imApi.createChatRoom(request.getName(), userChatId,
                    Collections.singletonList(userChatId), request.getName());
            voiceRoom = VoiceRoom.create(request.getName(), chatRoomId, request.getIsPrivate(),
                    request.getPassword(), request.getAllowFreeJoinMic(),
                    request.getType(), uid, request.getSoundEffect());
            voiceRoomMicService.initMic(voiceRoom.getChatroomId(), voiceRoom.getOwner());
            try {
                save(voiceRoom);
            } catch (Exception e) {
                log.error("save voice room failed | room={}, err=", voiceRoom, e);
                // todo voiceRoomMicService.clear(voiceRoom.getChatroomId());
                imApi.deleteChatRoom(voiceRoom.getChatroomId());
                throw e;
            }
            Long clickCount = 0L;
            Long memberCount = 0L;
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

    @Override public VoiceRoomDTO getByRoomId(String roomId) {
        VoiceRoom voiceRoom = findByRoomId(roomId);
        if (voiceRoom == null) {
            throw new RoomNotFoundException(String.format("room %s not found", roomId));
        }
        UserDTO userDTO = userService.getByUid(voiceRoom.getOwner());
        Long memberCount = getMemberCount(voiceRoom.getRoomId());
        Long clickCount = getClickCount(voiceRoom.getRoomId());
        PageInfo<UserDTO> pageInfo =
                voiceRoomUserService.findPageByRoomId(voiceRoom.getRoomId(), null, 10);

        List<GiftRecord> records =
                giftRecordService.getRankingListByRoomId(voiceRoom.getRoomId(),
                        voiceRoom.getOwner(), rankingLength);
        List<GiftRecordDTO> list = new ArrayList<>();
        if (records != null && !records.isEmpty()) {
            ArrayList<String> uidList = records.stream().map(GiftRecord::getUid).distinct()
                    .collect(Collectors.toCollection(Lists::newArrayList));
            Map<String, UserDTO> userDTOMap = userService.findByUidList(uidList);
            list = records.stream().map(giftRecord -> {
                UserDTO dto = userDTOMap.get(giftRecord.getUid());
                return new GiftRecordDTO(dto.getName(), dto.getPortrait(), giftRecord.getAmount());
            }).collect(Collectors.toList());
        }
        VoiceRoomDTO voiceRoomDTO = VoiceRoomDTO.from(voiceRoom, userDTO, memberCount, clickCount);
        return voiceRoomDTO.toBuilder().memberList(pageInfo.getList())
                .rankingList(list)
                .build();
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
            // todo imApi.setAnnouncement(voiceRoom.getChatroomId(), request.getAnnouncement());
        }
        updateById(voiceRoom);
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
        baseMapper.deleteById(voiceRoom.getId());
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

    public VoiceRoom findByRoomId(String roomId) {
        LambdaQueryWrapper<VoiceRoom> queryWrapper =
                new LambdaQueryWrapper<VoiceRoom>().eq(VoiceRoom::getRoomId, roomId);
        return baseMapper.selectOne(queryWrapper);
    }
}
