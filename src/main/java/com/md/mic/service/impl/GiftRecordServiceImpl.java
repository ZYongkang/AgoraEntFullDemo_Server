package com.md.mic.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.mic.model.GiftRecord;
import com.md.mic.repository.GiftRecordMapper;
import com.md.mic.service.GiftRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GiftRecordServiceImpl extends ServiceImpl<GiftRecordMapper, GiftRecord> implements
        GiftRecordService {

}
