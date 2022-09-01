package com.md.mic.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easemob.im.server.api.metadata.chatroom.get.ChatRoomMetadataGetResponse;
import com.md.common.im.ImApi;
import com.md.mic.model.MicApplyUser;
import com.md.mic.pojos.*;
import com.md.mic.repository.MicApplyUserMapper;
import com.md.mic.service.MicApplyUserService;
import com.md.mic.service.UserService;
import com.md.mic.service.VoiceRoomMicService;
import com.md.service.common.ErrorCodeEnum;
import com.md.service.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
public class MicApplyUserServiceImpl extends ServiceImpl<MicApplyUserMapper, MicApplyUser>
        implements MicApplyUserService {

    @Autowired
    private ImApi imApi;

    @Autowired
    private UserService userService;

    @Autowired
    private VoiceRoomMicService voiceRoomMicService;

    @Override
    public Boolean addMicApply(String uid, String roomId, AddMicApplyRequest request,Boolean freeMic) {
        Integer micIndex = request == null ? null : request.getMicIndex();
        if(!freeMic){
            try {
                MicApplyUser micApplyUser = new MicApplyUser();
                if (micIndex != null) {
                    micApplyUser.setMicIndex(micIndex);
                }
                micApplyUser.setRoomNo(roomId);
                micApplyUser.setUserNo(uid);
                this.save(micApplyUser);
                return Boolean.TRUE;
            } catch (Exception e) {
                log.error("addMicApply error,userNo:{},roomId:{}", uid, roomId, e);
                throw new BaseException(ErrorCodeEnum.add_mic_apply_error);
            }
        }else{
            if(micIndex==null){
                throw new BaseException(ErrorCodeEnum.mic_index_is_not_null);
            }
            return this.voiceRoomMicService.setRoomMicInfo(roomId,uid,micIndex,Boolean.FALSE);
        }

    }

    @Override
    public void deleteMicApply(String uid, String roomId) {

        QueryWrapper<MicApplyUser> wrapper = new QueryWrapper<>();
        wrapper.eq("user_no", uid);
        wrapper.eq("room_no", roomId);
        int count = this.baseMapper.delete(wrapper);
        if (count == 0) {
            throw new BaseException(ErrorCodeEnum.no_mic_apply_record);
        }

    }



    @Override
    public Boolean agreeApply(String roomId, String uid) {

        QueryWrapper<MicApplyUser> wrapper = new QueryWrapper<>();
        wrapper.eq("user_no", uid);
        wrapper.eq("room_no", roomId);
        MicApplyUser micApplyUser=this.getOne(wrapper);
        if(micApplyUser==null){
            throw new BaseException(ErrorCodeEnum.no_mic_apply_record);
        }

        Integer micIndex=micApplyUser.getMicIndex();

        return voiceRoomMicService.setRoomMicInfo(roomId,uid,micIndex,Boolean.TRUE);


    }

}
