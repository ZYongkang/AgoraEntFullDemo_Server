package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.common.im.ImApi;
import com.md.mic.common.config.GiftId;
import com.md.mic.common.constants.CustomEventType;
import com.md.mic.exception.UserNotInRoomException;
import com.md.mic.model.EasemobUser;
import com.md.mic.model.GiftRecord;
import com.md.mic.model.VoiceRoom;
import com.md.mic.model.VoiceRoomUser;
import com.md.mic.repository.GiftRecordMapper;
import com.md.mic.service.EasemobUserService;
import com.md.mic.service.GiftRecordService;
import com.md.mic.service.VoiceRoomService;
import com.md.mic.service.VoiceRoomUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GiftRecordServiceImpl extends ServiceImpl<GiftRecordMapper, GiftRecord> implements
        GiftRecordService {

    @Resource
    private VoiceRoomService voiceRoomService;

    @Resource
    private ImApi imApi;

    @Resource
    private EasemobUserService easemobUserService;

    @Resource
    private VoiceRoomUserService voiceRoomUserService;

    @Override
    public List<GiftRecord> getRankingListByRoomId(String roomId, String uid, String toUid, int limit) {
        VoiceRoomUser voiceRoomUser = voiceRoomUserService.findByRoomIdAndUid(roomId, uid);
        if (voiceRoomUser == null) {
            VoiceRoom voiceRoom = voiceRoomService.findByRoomId(roomId);
            if (!voiceRoom.getOwner().equals(uid)) {
                throw new UserNotInRoomException();
            }
        }
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
        VoiceRoom voiceRoom = voiceRoomService.findByRoomId(roomId);
        if (StringUtils.isBlank(toUid)) {
            toUid = voiceRoom.getOwner();
        }
        VoiceRoomUser voiceRoomUser = voiceRoomUserService.findByRoomIdAndUid(roomId, uid);
        if (voiceRoomUser == null && !voiceRoom.getOwner().equals(uid)) {
            throw new UserNotInRoomException();
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
        EasemobUser user = easemobUserService.getByUid(uid);
        Map<String, Object> customExt = new HashMap<>();
        customExt.put(giftId.toString(), String.valueOf(num));
        imApi.sendChatRoomCustomMessage(user.getChatId(), voiceRoom.getChatroomId(),
                CustomEventType.SEND_GIFT.getValue(), customExt, new HashMap<>());
    }

}
