package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.MicApplyUser;
import com.md.mic.pojos.AddMicApplyRequest;
import com.md.mic.pojos.MicApplyDTO;
import com.md.mic.pojos.PageInfo;
import com.md.mic.pojos.VoiceRoomDTO;

public interface MicApplyUserService extends IService<MicApplyUser> {

    Boolean addMicApply(String uid, VoiceRoomDTO roomInfo, AddMicApplyRequest request);

    void deleteMicApply(String uid, String roomId);

    Boolean agreeApply(VoiceRoomDTO roomInfo, String uid);

    PageInfo<MicApplyDTO> getByPage(String roomId, String cursor, Integer limit);
}
