package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.md.mic.model.EasemobUser;
import com.md.mic.repository.EasemobUserMapper;
import com.md.mic.service.EasemobUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;

@Slf4j
@Service
public class EasemobUserServiceImpl extends ServiceImpl<EasemobUserMapper, EasemobUser>
        implements EasemobUserService {

    @Resource(name = "voiceRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Value("${voice.room.redis.cache.ttl:PT1H}")
    private Duration ttl;

    @Resource
    private ObjectMapper objectMapper;

    @Override public EasemobUser getByUid(String uid) {
        Boolean hasKey = redisTemplate.hasKey(key(uid));
        EasemobUser easemobUser = null;
        if (Boolean.TRUE.equals(hasKey)) {
            String easemobUserStr = redisTemplate.opsForValue().get(key(uid));
            try {
                easemobUser = objectMapper.readValue(easemobUserStr, EasemobUser.class);
            } catch (JsonProcessingException e) {
                log.error("parse easemob user json cache failed | uid={}, str={}, e=", uid,
                        easemobUserStr, e);
            }
        }
        if (easemobUser == null) {
            LambdaQueryWrapper<EasemobUser> queryWrapper =
                    new LambdaQueryWrapper<EasemobUser>().eq(EasemobUser::getUid, uid);
            easemobUser = this.baseMapper.selectOne(queryWrapper);
            if (easemobUser != null) {
                String json;
                try {
                    json = objectMapper.writeValueAsString(easemobUser);
                    redisTemplate.opsForValue().set(key(uid), json, ttl);
                } catch (JsonProcessingException e) {
                    log.error("write easemob user json cache failed | uid={}, easemobUser={}, e=", uid,
                            easemobUser, e);
                }
            }
        }
        return easemobUser;
    }

    private String key(String uid) {
        return String.format("voiceRoom:easemobUser:uid:%s", uid);
    }

}
