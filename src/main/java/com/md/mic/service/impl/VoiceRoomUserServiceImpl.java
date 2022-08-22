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
import com.md.mic.model.VoiceRoomUser;
import com.md.mic.pojos.PageInfo;
import com.md.mic.pojos.UserDTO;
import com.md.mic.repository.VoiceRoomUserMapper;
import com.md.mic.service.UserService;
import com.md.mic.service.VoiceRoomService;
import com.md.mic.service.VoiceRoomUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VoiceRoomUserServiceImpl extends ServiceImpl<VoiceRoomUserMapper, VoiceRoomUser>
        implements VoiceRoomUserService {

    @Resource
    private UserService userService;

    @Resource
    private VoiceRoomService voiceRoomService;

    @Resource(name = "voiceRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private ImApi imApi;

    @Resource
    private EncryptionUtil encryptionUtil;

    @Value("${voice.room.redis.cache.ttl:PT1H}")
    private Duration ttl;

    @Override
    @Transactional
    public void deleteByRoomId(String roomId) {
        LambdaQueryWrapper<VoiceRoomUser> queryWrapper =
                new LambdaQueryWrapper<VoiceRoomUser>().eq(VoiceRoomUser::getRoomId, roomId);
        List<VoiceRoomUser> voiceRoomUserList = baseMapper.selectList(queryWrapper);
        if (voiceRoomUserList == null || voiceRoomUserList.isEmpty()) {
            return;
        }
        List<Integer> idList =
                voiceRoomUserList.stream().map(VoiceRoomUser::getId).collect(Collectors.toList());
        baseMapper.deleteBatchIds(idList);
        cleanMemberCount(roomId);
        cleanClickCount(roomId);
    }

    @Override
    public PageInfo<UserDTO> findPageByRoomId(String roomId, String cursor, Integer limit) {
        VoiceRoom voiceRoom = voiceRoomService.findByRoomId(roomId);
        if (voiceRoom == null) {
            throw new RoomNotFoundException(String.format("room %s not found", roomId));
        }
        Long total = baseMapper.selectCount(
                new LambdaQueryWrapper<VoiceRoomUser>().eq(VoiceRoomUser::getRoomId, roomId));
        int limitSize = limit + 1;
        List<VoiceRoomUser> voiceRoomUserList;
        if (StringUtils.isBlank(cursor)) {
            LambdaQueryWrapper<VoiceRoomUser> queryWrapper =
                    new LambdaQueryWrapper<VoiceRoomUser>()
                            .eq(VoiceRoomUser::getRoomId,roomId)
                            .orderByDesc(VoiceRoomUser::getId)
                            .last(" limit " + limitSize);
            voiceRoomUserList = baseMapper.selectList(queryWrapper);
        } else {
            String s = new String(
                    Base64.getUrlDecoder().decode(cursor.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
            int id = Integer.parseInt(s);
            LambdaQueryWrapper<VoiceRoomUser> queryWrapper =
                    new LambdaQueryWrapper<VoiceRoomUser>()
                            .eq(VoiceRoomUser::getRoomId, roomId)
                            .le(VoiceRoomUser::getId, id)
                            .orderByDesc(VoiceRoomUser::getId)
                            .last(" limit " + limitSize);
            voiceRoomUserList = baseMapper.selectList(queryWrapper);
        }

        if (voiceRoomUserList == null || voiceRoomUserList.isEmpty()) {
            return new PageInfo<>();
        }

        if (voiceRoomUserList.size() == limitSize) {
            VoiceRoomUser voiceRoomUser = voiceRoomUserList.get(limitSize - 1);
            Integer id = voiceRoomUser.getId();
            cursor = Base64.getUrlEncoder()
                    .encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
            voiceRoomUserList.remove(voiceRoomUser);
        } else {
            cursor = null;
        }
        List<String> uidList =
                voiceRoomUserList.stream().map(VoiceRoomUser::getUid).collect(Collectors.toList());
        Map<String, UserDTO> userDTOMap = userService.findByUidList(uidList);
        List<UserDTO> userDTOList = voiceRoomUserList.stream()
                .map(voiceRoomUser -> userDTOMap.get(voiceRoomUser.getUid()))
                .collect(Collectors.toList());
        PageInfo<UserDTO> pageInfo = new PageInfo<>();
        pageInfo.setCursor(cursor);
        pageInfo.setTotal(total);
        pageInfo.setList(userDTOList);
        return pageInfo;
    }

    @Override public VoiceRoomUser findByRoomIdAndUid(String roomId, String uid) {
        Boolean hasKey = redisTemplate.hasKey(key(roomId, uid));
        VoiceRoomUser voiceRoomUser = null;
        if (Boolean.TRUE.equals(hasKey)) {
            String json = redisTemplate.opsForValue().get(key(roomId, uid));
            try {
                voiceRoomUser = objectMapper.readValue(json, VoiceRoomUser.class);
            } catch (JsonProcessingException e) {
                log.error("parse voice room user json cache failed | roomId={}, uid={},"
                        + " json={}, e=", uid, json, e);
            }
        }
        if (voiceRoomUser == null) {
            LambdaQueryWrapper<VoiceRoomUser> queryWrapper =
                    new LambdaQueryWrapper<VoiceRoomUser>().eq(VoiceRoomUser::getRoomId, roomId)
                            .eq(VoiceRoomUser::getUid, uid);
            voiceRoomUser = baseMapper.selectOne(queryWrapper);
            if (voiceRoomUser != null) {
                String json = null;
                try {
                    json = objectMapper.writeValueAsString(voiceRoomUser);
                    redisTemplate.opsForValue().set(key(roomId, uid), json, ttl);
                } catch (JsonProcessingException e) {
                    log.error("write voice room user json cache failed | roomId={}, uid={},"
                            + " voiceRoomUser={}, e=", uid, voiceRoomUser, e);
                }
            }
        }
        return voiceRoomUser;
    }

    @Override
    @Transactional
    public VoiceRoomUser addVoiceRoomUser(String roomId, String uid, String password) {
        VoiceRoom voiceRoom = voiceRoomService.findByRoomId(roomId);
        if (uid.equals(voiceRoom.getOwner())) {
            return VoiceRoomUser.builder().roomId(roomId).uid(uid).build();
        }
        if (Boolean.TRUE.equals(voiceRoom.getIsPrivate())) {
            if (StringUtils.isBlank(password)) {
                throw new IllegalArgumentException("private room name not allow empty");
            }
            boolean checkResult = encryptionUtil.validPassword(password, voiceRoom.getPassword());
            if (!checkResult) {
                throw new VoiceRoomSecurityException("wrong password");
            }
        }
        LambdaQueryWrapper<VoiceRoomUser> queryWrapper =
                new LambdaQueryWrapper<VoiceRoomUser>().eq(VoiceRoomUser::getRoomId, roomId)
                        .eq(VoiceRoomUser::getUid, uid);
        VoiceRoomUser voiceRoomUser = baseMapper.selectOne(queryWrapper);
        if (voiceRoomUser == null) {
            voiceRoomUser = VoiceRoomUser.create(roomId, uid);
            save(voiceRoomUser);
            incrClickCount(roomId);
            incrMemberCount(roomId);
        }
        return voiceRoomUser;
    }

    @Override
    @Transactional
    public void deleteVoiceRoomUser(String roomId, String uid, Boolean isSuccess) {
        VoiceRoom voiceRoom = voiceRoomService.findByRoomId(roomId);
        if (voiceRoom == null) {
            return;
        }
        if (uid.equals(voiceRoom.getOwner())) {
            voiceRoomService.deleteByRoomId(roomId, uid);
        } else {
            VoiceRoomUser voiceRoomUser = findByRoomIdAndUid(roomId, uid);
            if (voiceRoomUser != null) {
                baseMapper.deleteById(voiceRoomUser);
                decrMemberCount(roomId);
                redisTemplate.delete(key(roomId, uid));
                if (Boolean.FALSE.equals(isSuccess)) {
                    UserDTO user = userService.getByUid(uid);
                    imApi.removeChatRoomMember(voiceRoom.getChatroomId(), user.getChatUid());
                }
            }
        }
    }

    @Override
    @Transactional
    public void kickVoiceRoomUser(String roomId, String ownerUid, String kickUid) {
        VoiceRoom voiceRoom = voiceRoomService.findByRoomId(roomId);
        if (!ownerUid.equals(voiceRoom.getOwner())) {
            throw new VoiceRoomSecurityException("not the owner can't operate");
        }
        if (ownerUid.equals(kickUid)) {
            throw new VoiceRoomSecurityException("the owner cannot be kicked out of the room");
        }
        VoiceRoomUser voiceRoomUser = findByRoomIdAndUid(roomId, kickUid);
        if (voiceRoomUser != null) {
            baseMapper.deleteById(voiceRoomUser);
            decrMemberCount(roomId);
            redisTemplate.delete(key(roomId, kickUid));
            UserDTO kickUser = userService.getByUid(kickUid);
            imApi.kickChatroomMember(voiceRoom.getChatroomId(), kickUser.getChatUid());
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

    private Long decrMemberCount(String roomId) {
        String key = String.format("room:voice:%s:clickCount", roomId);
        return redisTemplate.opsForValue().increment(key, -1L);
    }

    private void cleanMemberCount(String roomId) {
        String key = String.format("room:voice:%s:clickCount", roomId);
        redisTemplate.delete(key);
    }

    private void cleanClickCount(String roomId) {
        String key = String.format("room:voice:%s:memberCount", roomId);
        redisTemplate.delete(key);
    }

    private String key(String roomId, String uid) {
        return String.format("voiceRoomUser:room:%s:user:uid:%s", roomId, uid);
    }
}
