package com.md.mic.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.mic.model.MicApplyUser;
import com.md.mic.repository.MicApplyUserMapper;
import com.md.mic.service.MicApplyUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MicApplyUserServiceImpl extends ServiceImpl<MicApplyUserMapper, MicApplyUser>
        implements MicApplyUserService {

}
