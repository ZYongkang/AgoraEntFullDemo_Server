package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.UserThirdAccount;

public interface UserThirdAccountService extends IService<UserThirdAccount> {

    /**
     * 根据uid获取三方账户信息
     * @param uid
     * @return
     */
    UserThirdAccount getByUid(String uid);
}
