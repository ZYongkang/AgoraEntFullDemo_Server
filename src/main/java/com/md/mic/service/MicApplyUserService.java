package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.MicApplyUser;
import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.PageInfo;
import com.md.mic.pojos.UserDTO;
import com.md.mic.pojos.vo.MicApplyVO;

public interface MicApplyUserService extends IService<MicApplyUser> {

    Boolean addMicApply(UserDTO user, VoiceRoom roomInfo, Integer micIndex);

    void deleteMicApply(String uid, VoiceRoom roomInfo, Boolean sendNotify);

    Boolean agreeApply(VoiceRoom roomInfo, String uid);

    PageInfo<MicApplyVO> getByPage(String roomId, String cursor, Integer limit);

    Boolean refuseApply(VoiceRoom roomInfo, String uid, Integer micIndex);
}
