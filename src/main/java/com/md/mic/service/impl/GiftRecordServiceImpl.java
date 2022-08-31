package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.mic.model.GiftRecord;
import com.md.mic.pojos.GiftRecordDTO;
import com.md.mic.repository.GiftRecordMapper;
import com.md.mic.service.GiftRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class GiftRecordServiceImpl extends ServiceImpl<GiftRecordMapper, GiftRecord> implements
        GiftRecordService {

    @Override
    public List<GiftRecord> getRankingListByRoomId(String roomId, String toUid, int limit) {
        LambdaQueryWrapper<GiftRecord> queryWrapper =
                new LambdaQueryWrapper<GiftRecord>().eq(GiftRecord::getRoomId, roomId)
                        .eq(GiftRecord::getToUid, toUid)
                        .orderByDesc(GiftRecord::getAmount)
                        .last(" limit " + limit);
        return baseMapper.selectList(queryWrapper);
    }

}
