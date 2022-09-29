package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.mic.common.config.GiftId;
import com.md.mic.exception.UserNotInRoomException;
import com.md.mic.model.GiftRecord;
import com.md.mic.model.VoiceRoom;
import com.md.mic.model.VoiceRoomUser;
import com.md.mic.repository.GiftRecordMapper;
import com.md.mic.service.GiftRecordService;
import com.md.mic.service.VoiceRoomService;
import com.md.mic.service.VoiceRoomUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class GiftRecordServiceImpl extends ServiceImpl<GiftRecordMapper, GiftRecord> implements
        GiftRecordService {

    @Resource
    private VoiceRoomService voiceRoomService;

    @Resource
    private VoiceRoomUserService voiceRoomUserService;

    @Resource(name = "voiceRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Override
    public List<GiftRecord> getRankingListByRoomId(String roomId, String toUid, int limit) {
        LambdaQueryWrapper<GiftRecord> queryWrapper =
                new LambdaQueryWrapper<GiftRecord>()
                        .eq(GiftRecord::getRoomId, roomId)
                        .eq(GiftRecord::getToUid, toUid)
                        .orderByDesc(GiftRecord::getAmount)
                        .last(" limit " + limit);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public void addGiftRecord(String roomId, String uid, GiftId giftId, Integer num, String toUid) {
        VoiceRoom voiceRoom = voiceRoomService.findByRoomId(roomId);
        if (StringUtils.isBlank(toUid)) {
            toUid = voiceRoom.getOwner();
        }
        VoiceRoomUser toVoiceRoomUser = voiceRoomUserService.findByRoomIdAndUid(roomId, toUid);

        if (toVoiceRoomUser == null && !toUid.equals(voiceRoom.getOwner())) {
            throw new UserNotInRoomException("receiver not in the room");
        }

        VoiceRoomUser voiceRoomUser = voiceRoomUserService.findByRoomIdAndUid(roomId, uid);
        if (voiceRoomUser == null && !voiceRoom.getOwner().equals(uid)) {
            throw new UserNotInRoomException();
        }
        Long amount = giftId.getAmount() * num;
        incrRoomGiftAmount(roomId, amount);
        LambdaQueryWrapper<GiftRecord> queryWrapper =
                new LambdaQueryWrapper<GiftRecord>()
                        .eq(GiftRecord::getRoomId, roomId)
                        .eq(GiftRecord::getUid, uid)
                        .eq(GiftRecord::getToUid, toUid);
        GiftRecord giftRecord = baseMapper.selectOne(queryWrapper);
        if (giftRecord == null) {
            giftRecord = GiftRecord.create(roomId, uid, toUid, amount);
            save(giftRecord);
        } else {
            giftRecord = giftRecord.addAmount(amount);
            updateById(giftRecord);
        }
    }

    @Override
    public Long getRoomGiftAmount(String roomId) {
        try {
            return redisTemplate.opsForValue().increment(key(roomId), 0L);
        } catch (Exception e) {
            log.error("get room gift amount failed", e);
            return 0L;
        }
    }

    public void incrRoomGiftAmount(String roomId, Long amount) {
        try {
            redisTemplate.opsForValue().increment(key(roomId), amount);
        } catch (Exception e) {
            log.error("get room gift amount failed", e);
        }
    }

    private String key(String roomId) {
        return String.format("voiceRoom:%s:gift:amount", roomId);
    }

}
