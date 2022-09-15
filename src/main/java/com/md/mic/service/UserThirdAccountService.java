package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.UserThirdAccount;

public interface UserThirdAccountService extends IService<UserThirdAccount> {

    UserThirdAccount getByUid(String uid);
}
