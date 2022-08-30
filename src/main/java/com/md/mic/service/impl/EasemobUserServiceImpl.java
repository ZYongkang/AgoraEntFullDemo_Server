package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.mic.model.EasemobUser;
import com.md.mic.repository.EasemobUserMapper;
import com.md.mic.service.EasemobUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EasemobUserServiceImpl extends ServiceImpl<EasemobUserMapper, EasemobUser>
        implements EasemobUserService {

    @Override public EasemobUser getByUid(String uid) {
        LambdaQueryWrapper<EasemobUser> queryWrapper =
                new LambdaQueryWrapper<EasemobUser>().eq(EasemobUser::getUid, uid);
        return this.baseMapper.selectOne(queryWrapper);
    }

}
