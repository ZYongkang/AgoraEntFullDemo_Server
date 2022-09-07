package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.mic.common.config.GiftId;
import com.md.mic.model.GiftRecord;
import com.md.mic.model.VoiceRoom;
import com.md.mic.repository.GiftRecordMapper;
import com.md.mic.service.GiftRecordService;
import com.md.mic.service.VoiceRoomService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class GiftRecordServiceImpl extends ServiceImpl<GiftRecordMapper, GiftRecord> implements
        GiftRecordService {

    @Resource
    private VoiceRoomService voiceRoomService;

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
    @Transactional
    public void addGiftRecord(String roomId, String uid, GiftId giftId, Integer num, String toUid) {
        //todo 可以使用zset来做排行榜
        if (StringUtils.isBlank(toUid)) {
            VoiceRoom voiceRoom = voiceRoomService.findByRoomId(roomId);
            toUid = voiceRoom.getOwner();
        }
        Long amount = giftId.getAmount() * num;
        LambdaQueryWrapper<GiftRecord> queryWrapper =
                new LambdaQueryWrapper<GiftRecord>().eq(GiftRecord::getRoomId, roomId)
                        .eq(GiftRecord::getUid, uid).eq(GiftRecord::getToUid, toUid);
        GiftRecord giftRecord = baseMapper.selectOne(queryWrapper);
        if (giftRecord == null) {
            giftRecord = GiftRecord.create(roomId, uid, toUid, amount);
            save(giftRecord);
        } else {
            giftRecord = giftRecord.addAmount(amount);
            updateById(giftRecord);
        }
        //todo 向im 发送送礼物消息
    }

}
