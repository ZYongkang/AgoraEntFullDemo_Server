package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.common.config.GiftId;
import com.md.mic.model.GiftRecord;

import java.util.List;

public interface GiftRecordService extends IService<GiftRecord> {

    List<GiftRecord> getRankingListByRoomId(String roomId, String uid, String toUid, int limit);

    void addGiftRecord(String roomId, String uid, GiftId giftId, Integer num, String toUid);
}
