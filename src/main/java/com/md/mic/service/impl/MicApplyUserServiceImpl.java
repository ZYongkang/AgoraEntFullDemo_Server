package com.md.mic.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.common.im.ImApi;
import com.md.mic.common.constants.CustomEventType;
import com.md.mic.exception.MicApplyException;
import com.md.mic.exception.MicApplyRecordNotFoundException;
import com.md.mic.exception.MicApplyRepeatException;
import com.md.mic.exception.MicIndexNullException;
import com.md.mic.model.MicApplyUser;
import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.*;
import com.md.mic.repository.MicApplyUserMapper;
import com.md.mic.service.MicApplyUserService;
import com.md.mic.service.UserService;
import com.md.mic.service.VoiceRoomMicService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MicApplyUserServiceImpl extends ServiceImpl<MicApplyUserMapper, MicApplyUser>
        implements MicApplyUserService {

    @Value("${local.zone.offset:+8}")
    private String zoneOffset;

    @Autowired
    private ImApi imApi;

    @Autowired
    private UserService userService;

    @Autowired
    private VoiceRoomMicService voiceRoomMicService;

    @Override
    public Boolean addMicApply(String uid, VoiceRoom roomInfo, AddMicApplyRequest request) {
        Integer micIndex = request == null ? null : request.getMicIndex();
        String roomId = roomInfo.getRoomId();
        if (!roomInfo.getAllowedFreeJoinMic()) {
            try {
                MicApplyUser micApplyUser = new MicApplyUser();
                if (micIndex != null) {
                    micApplyUser.setMicIndex(micIndex);
                }
                micApplyUser.setRoomId(roomInfo.getRoomId());
                micApplyUser.setUid(uid);
                this.save(micApplyUser);

                UserDTO applyUser = userService.getByUid(uid);

                Map<String, Object> customExtensions = new HashMap<>();
                customExtensions.put("user", JSONObject.toJSONString(applyUser));
                customExtensions.put("mic_index", micIndex.toString());
                customExtensions.put("room_id", roomInfo.getRoomId());
                this.imApi.sendUserCustomMessage(applyUser.getChatUid(),
                        roomInfo.getOwner(),
                        CustomEventType.APPLY_SITE.getValue(), customExtensions, new HashMap<>());

                return Boolean.TRUE;
            } catch (Exception e) {
                log.error("addMicApply error,userNo:{},roomId:{}", uid, roomId, e);
                if(e instanceof SQLIntegrityConstraintViolationException){
                    throw new MicApplyRepeatException();
                }
                throw new MicApplyException();
            }
        } else {
            if (micIndex == null) {
                throw new MicIndexNullException();
            }
            return this.voiceRoomMicService.setRoomMicInfo(roomInfo.getChatroomId(), uid, micIndex, Boolean.FALSE);
        }

    }

    @Override
    public void deleteMicApply(String uid, String roomId) {
        LambdaQueryWrapper<MicApplyUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MicApplyUser::getUid,uid);
        wrapper.eq(MicApplyUser::getRoomId,uid);
        int count = this.baseMapper.delete(wrapper);
        if (count == 0) {
            throw new MicApplyRecordNotFoundException();
        }

    }

    @Override
    public Boolean agreeApply(VoiceRoom roomInfo, String uid) {

        String roomId = roomInfo.getRoomId();
        LambdaQueryWrapper<MicApplyUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MicApplyUser::getUid,uid);
        wrapper.eq(MicApplyUser::getRoomId,uid);
        MicApplyUser micApplyUser = this.getOne(wrapper);
        if (micApplyUser == null) {
            throw new MicApplyRecordNotFoundException();
        }

        Integer micIndex = micApplyUser.getMicIndex();

        return voiceRoomMicService.setRoomMicInfo(roomInfo.getChatroomId(), uid, micIndex, Boolean.TRUE);

    }

    @Override
    public Boolean refuseApply(VoiceRoom roomInfo, String uid, Integer micIndex) {

        deleteMicApply(uid,roomInfo.getRoomId());

        UserDTO applyUser=this.userService.getByUid(uid);
        UserDTO ownerUser=this.userService.getByUid(roomInfo.getOwner());

        Map<String, Object> customExtensions = new HashMap<>();
        customExtensions.put("user", JSONObject.toJSONString(applyUser));
        if(micIndex!=null){
            customExtensions.put("mic_index", micIndex.toString());
        }
        customExtensions.put("room_id", roomInfo.getRoomId());
        this.imApi.sendUserCustomMessage(ownerUser.getChatUid(),
                applyUser.getChatUid(),
                CustomEventType.APPLY_REFUSED.getValue(), customExtensions, new HashMap<>());

        return Boolean.TRUE;

    }


    @Override public PageInfo<MicApplyDTO> getByPage(String roomId, String cursor, Integer limit) {
        List<MicApplyUser> micApplyUser;
        int limitSize = limit + 1;
        Long total = baseMapper.selectCount(new LambdaQueryWrapper<>());
        if (StringUtils.isBlank(cursor)) {
            LambdaQueryWrapper<MicApplyUser> queryWrapper =
                    new LambdaQueryWrapper<MicApplyUser>().eq(MicApplyUser::getRoomId,roomId).orderByDesc(MicApplyUser::getId)
                            .last(" limit " + limitSize);
            micApplyUser = baseMapper.selectList(queryWrapper);
        } else {
            String s = new String(
                    Base64.getUrlDecoder().decode(cursor.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
            int id = Integer.parseInt(s);
            LambdaQueryWrapper<MicApplyUser> queryWrapper =
                    new LambdaQueryWrapper<MicApplyUser>().eq(MicApplyUser::getRoomId,roomId).le(MicApplyUser::getId, id)
                            .orderByDesc(MicApplyUser::getId)
                            .last(" limit " + limitSize);
            micApplyUser = baseMapper.selectList(queryWrapper);
        }
        if (micApplyUser.size() == limitSize) {
            MicApplyUser micApply = micApplyUser.get(limitSize - 1);
            Integer id = micApply.getId();
            cursor = Base64.getUrlEncoder()
                    .encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
            micApplyUser.remove(micApply);
        } else {
            cursor = null;
        }
        List<String> ownerUidList =
                micApplyUser.stream().map(MicApplyUser::getUid).collect(Collectors.toList());
        Map<String, UserDTO> ownerMap =new HashMap<>();
        if(!CollectionUtils.isEmpty(ownerUidList)){
            ownerMap=userService.findByUidList(ownerUidList);
        }

        List<MicApplyDTO> list = new ArrayList<>();
        for(MicApplyUser applyUser:micApplyUser){
            UserDTO userDTO = ownerMap.get(applyUser.getUid());
            long createdAt = applyUser.getCreatedAt().toInstant(ZoneOffset.of(zoneOffset))
                    .toEpochMilli();
            list.add(MicApplyDTO.builder().user(userDTO).index(applyUser.getMicIndex())
                    .createAt(createdAt).build());
        }
        PageInfo<MicApplyDTO> pageInfo = new PageInfo<>();
        pageInfo.setCursor(cursor);
        pageInfo.setTotal(total);
        pageInfo.setList(list);
        return pageInfo;
    }
}
