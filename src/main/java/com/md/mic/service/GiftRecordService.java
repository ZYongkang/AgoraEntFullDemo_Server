package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.GiftRecord;
import com.md.mic.pojos.GiftRecordDTO;

import java.util.List;

public interface GiftRecordService extends IService<GiftRecord> {

    List<GiftRecord> getRankingListByRoomId(String roomId, String toUid, int limit);
}
