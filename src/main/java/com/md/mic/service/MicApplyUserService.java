package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.MicApplyUser;
import com.md.mic.pojos.AddMicApplyRequest;
import com.md.mic.pojos.MicInfo;

import java.util.List;

public interface MicApplyUserService extends IService<MicApplyUser> {

    void addMicApply(String uid, String roomId, AddMicApplyRequest request);

    void deleteMicApply(String uid, String roomId);

    Boolean agreeApply(String roomId, String uid);
}
