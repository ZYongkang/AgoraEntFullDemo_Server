package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.md.common.im.ImApi;
import com.md.mic.exception.UserNotFoundException;
import com.md.mic.model.EasemobUser;
import com.md.mic.model.User;
import com.md.mic.pojos.UserDTO;
import com.md.mic.repository.UserMapper;
import com.md.mic.service.EasemobUserService;
import com.md.mic.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private ImApi imApi;

    @Resource
    private EasemobUserService easemobUserService;

    @Resource(name = "voiceRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Value("${voice.room.redis.cache.ttl:PT1H}")
    private Duration ttl;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public UserDTO loginDevice(String deviceId, String name, String portrait) {
        LambdaQueryWrapper<User> queryWrapper =
                new LambdaQueryWrapper<User>().eq(User::getDeviceId, deviceId);
        User user = getOne(queryWrapper);
        EasemobUser easemobUser;
        if (user == null) {
            String chatUid = String.format("u%s",
                    UUID.randomUUID().toString().replace("-", "")
                            .substring(1));
            user = User.create(name, deviceId, portrait);
            save(user);
            easemobUser = imApi.createUser(user.getUid(), chatUid);
            try {
                easemobUserService.save(easemobUser);
            } catch (Exception e) {
                log.error("save easemob user failed | err=", e);
                imApi.deleteUser(chatUid);
                throw e;
            }
        } else {
            String uid = user.getUid();
            easemobUser = easemobUserService.getOne(
                    new LambdaQueryWrapper<EasemobUser>().eq(EasemobUser::getUid, uid));
        }
        return UserDTO.builder().uid(user.getUid())
                .chatUid(easemobUser.getChatId())
                .chatUuid(easemobUser.getChatUuid())
                .name(user.getName())
                .portrait(user.getPortrait())
                .build();
    }

    @Override public Map<String, UserDTO> findByUidList(List<String> ownerUidList) {
        if (ownerUidList == null || ownerUidList.isEmpty()) {
            return new HashMap<>();
        }
        LambdaQueryWrapper<User> queryWrapper =
                new LambdaQueryWrapper<User>().in(User::getUid, ownerUidList);
        List<User> userList = baseMapper.selectList(queryWrapper);
        if (userList == null || userList.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, UserDTO> map = new HashMap<>(userList.size());
        for (User user : userList) {
            String uid = user.getUid();
            UserDTO userDTO = UserDTO.builder()
                    .uid(uid)
                    .portrait(user.getPortrait())
                    .name(user.getName())
                    .build();
            map.put(uid, userDTO);
        }
        return map;
    }

    @Override public UserDTO getByUid(String uid) {
        if (StringUtils.isBlank(uid)) {
            throw new IllegalArgumentException("uid must not be empty");
        }
        Boolean hasKey = redisTemplate.hasKey(key(uid));
        User user = null;
        if (Boolean.TRUE.equals(hasKey)) {
            String json = redisTemplate.opsForValue().get(key(uid));
            try {
                user = objectMapper.readValue(json, User.class);
            } catch (JsonProcessingException e) {
                log.error("parse user json cache failed | uid={}, json={}, e=", uid,
                        json, e);
            }
        }
        if (user == null) {
            LambdaQueryWrapper<User> queryWrapper =
                    new LambdaQueryWrapper<User>().eq(User::getUid, uid);
            user = baseMapper.selectOne(queryWrapper);
            try {
                String json = objectMapper.writeValueAsString(user);
                redisTemplate.opsForValue().set(key(uid), json, ttl);
            } catch (JsonProcessingException e) {
                log.error("write user json cache failed | uid={}, user={}, e=", uid,
                        user, e);
            }
        }
        if (user == null) {
            throw new UserNotFoundException("user " + uid + " not found");
        }
        EasemobUser easemobUser = easemobUserService.getByUid(uid);
        return UserDTO.from(user, easemobUser);
    }

    private String key(String uid) {
        return String.format("voiceRoom:user:uid:%s", uid);
    }

}
