package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.common.config.GiftId;
import com.md.mic.model.GiftRecord;

import java.util.List;

public interface GiftRecordService extends IService<GiftRecord> {

    /**
     * 获取房间打赏排行榜前100
     * @param roomId
     * @param toUid
     * @param limit
     * @return
     */
    List<GiftRecord> getRankingListByRoomId(String roomId, String toUid, int limit);

    /**
     * 添加打赏记录
     * @param roomId
     * @param uid
     * @param giftId
     * @param num
     * @param toUid
     */
    void addGiftRecord(String roomId, String uid, GiftId giftId, Integer num, String toUid);

    /**
     * 获取房间总打赏值
     * @param roomId
     * @return
     */
    public Long getRoomGiftAmount(String roomId);
}
