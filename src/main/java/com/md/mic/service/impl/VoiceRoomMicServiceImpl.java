package com.md.mic.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.easemob.im.server.api.metadata.chatroom.AutoDelete;
import com.easemob.im.server.api.metadata.chatroom.get.ChatRoomMetadataGetResponse;
import com.md.common.im.ImApi;
import com.md.mic.pojos.*;
import com.md.mic.service.UserService;
import com.md.mic.service.VoiceRoomMicService;
import com.md.service.common.ErrorCodeEnum;
import com.md.service.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class VoiceRoomMicServiceImpl implements VoiceRoomMicService {

    @Autowired
    private ImApi imApi;

    @Autowired
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    private static final String OPERATOR = "admin";

    private static final String METADATA_PREFIX_KEY = "mic";

    @Value("${voice.room.mic.count:9}")
    private int micCount;

    private List<String> allMics=new ArrayList<>();

    @PostConstruct
    public void init(){
        for(int index=0;index<micCount;index++){
            allMics.add(buildMicKey(index));
        }
    }

    @Override
    public List<MicInfo> getByRoomId(String roomId) {
        return getRoomMicInfo(roomId);
    }

    @Override
    public List<MicInfo> getRoomMicInfo(String roomId) {
        try {
            ChatRoomMetadataGetResponse chatRoomMetadataGetResponse =
                    imApi.listChatRoomMetadata(roomId, allMics);
            Map<String, String> metadata = chatRoomMetadataGetResponse.getMetadata();
            List<MicInfo> micInfo = buildMicInfo(metadata);
            return micInfo;
        } catch (Exception e) {
            log.error("getRoomMicInfo error,roomId:{}", roomId, e);
            return Collections.emptyList();
        }

    }

    @Override
    public Boolean setRoomMicInfoInOrder(String roomId, String uid, Integer micIndex) {
        List<MicInfo> micInfos = this.getRoomMicInfo(roomId);

        Optional<MicInfo> indexMic = micInfos.stream().filter((mic) -> {
                    return mic.getIndex() == micIndex;
                }
        ).findFirst();
        Boolean hasMic = false;
        if (indexMic.isPresent()) {
            if (indexMic.get().getStatus() == MicStatus.FREE.getStatus()) {
                try {
                    this.updateVoiceRoomMicInfo(roomId, uid, micIndex, MicOperateStatus.UP_MIC.getStatus(),Boolean.FALSE);
                    hasMic = true;
                } catch (Exception e) {

                    //按顺序上麦
                    for (int i = 1; i <= micCount-1; i++) {
                        try {
                            this.updateVoiceRoomMicInfo(roomId, uid, i,MicOperateStatus.UP_MIC.getStatus(),Boolean.FALSE);
                            hasMic = true;
                            break;
                        } catch (Exception innerE) {
                            log.warn("on mic failure,roomId:{},uid:{},index:{}", roomId, uid, i, e);
                        }

                    }

                }
            }
        } else {
            throw new BaseException(ErrorCodeEnum.mic_not_init);
        }
        return hasMic;

    }

    @Override
    public void closeMic(String uid, String roomId, Integer micIndex) {
        String metadataKey = buildMicKey(micIndex);
        Map<String, String> metadata =
                imApi.listChatRoomMetadata(roomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid()) || !micMetadataValue.getUid()
                    .equals(uid)) {
                throw new BaseException(ErrorCodeEnum.mic_not_belong_you);
            }
            if(micMetadataValue.getStatus() == MicStatus.NORMAL.getStatus()){
                 this.updateVoiceRoomMicInfo(roomId,uid,micIndex,MicOperateStatus.CLOSE_MIC.getStatus(),Boolean.FALSE);
            }else{
                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
            }

        } else {
            throw new BaseException(ErrorCodeEnum.mic_not_init);
        }
    }

    @Override
    public void openMic(String uid, String roomId, Integer micIndex) {

        String metadataKey = buildMicKey(micIndex);
        Map<String, String> metadata =
                imApi.listChatRoomMetadata(roomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid()) || !micMetadataValue.getUid()
                    .equals(uid)) {
                throw new BaseException(ErrorCodeEnum.mic_not_belong_you);
            }
            if(micMetadataValue.getStatus() == MicStatus.CLOSE.getStatus()){
                this.updateVoiceRoomMicInfo(roomId,uid,micIndex,MicOperateStatus.OPEN_MIC.getStatus(),Boolean.FALSE);
            }else{
                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
            }

        } else {
            throw new BaseException(ErrorCodeEnum.mic_not_init);
        }
    }

    @Override
    public void leaveMic(String uid, String roomId, Integer micIndex) {
        String metadataKey = buildMicKey(micIndex);
        Map<String, String> metadata =
                imApi.listChatRoomMetadata(roomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid()) || !micMetadataValue.getUid()
                    .equals(uid)) {
                throw new BaseException(ErrorCodeEnum.mic_not_belong_you);
            }

            this.updateVoiceRoomMicInfo(roomId,uid,micIndex,MicOperateStatus.LEAVE_MIC.getStatus(),Boolean.FALSE);


        } else {
            throw new BaseException(ErrorCodeEnum.mic_not_init);
        }
    }

    @Override
    public void muteMic(String roomId, Integer micIndex) {

        String metadataKey = buildMicKey(micIndex);
        Map<String, String> metadata =
                imApi.listChatRoomMetadata(roomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid()) || micMetadataValue.getStatus()==MicStatus.MUTE.getStatus()) {
                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
            }

            this.updateVoiceRoomMicInfo(roomId,null,micIndex,MicOperateStatus.MUTE_MIC.getStatus(),Boolean.TRUE);


        } else {
            throw new BaseException(ErrorCodeEnum.mic_not_init);
        }
    }

    @Override
    public void unMuteMic(String roomId, Integer micIndex) {
        String metadataKey = buildMicKey(micIndex);
        Map<String, String> metadata =
                imApi.listChatRoomMetadata(roomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid()) || micMetadataValue.getStatus()!=MicStatus.MUTE.getStatus()) {
                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
            }

            this.updateVoiceRoomMicInfo(roomId,null,micIndex,MicOperateStatus.UNMUTE_MIC.getStatus(),Boolean.TRUE);


        } else {
            throw new BaseException(ErrorCodeEnum.mic_not_init);
        }
    }

    private void updateVoiceRoomMicInfo(String roomId, String uid, Integer micIndex,
            Integer micOperateStatus,Boolean isAdminOperate) {
        String metadataKey = buildMicKey(micIndex);
        String redisLockKey=metadataKey+"_lock";
        Boolean isContinue = redisTemplate.opsForValue()
                .setIfAbsent(redisLockKey, metadataKey, Duration.ofMillis(5000));
        try{
            if(isContinue){
                Map<String, String> metadata =
                        imApi.listChatRoomMetadata(roomId, Arrays.asList(metadataKey)).getMetadata();
                if (metadata.containsKey(metadataKey)) {

                    MicMetadataValue micMetadataValue =
                            JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

                    Integer updateStatus=null;
                    String updateUid=micMetadataValue.getUid();

                    //麦有人的情况下、只允许对该人进行操作
                    if (!isAdminOperate&&!StringUtils.isEmpty(micMetadataValue.getUid()) && !micMetadataValue.getUid()
                            .equals(uid)) {
                        throw new BaseException(ErrorCodeEnum.mic_not_belong_you);
                    }

                    //上麦条件
                    if(micOperateStatus== MicOperateStatus.UP_MIC.getStatus()&&micMetadataValue.getStatus() != MicStatus.FREE.getStatus()){
                        throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                    }else{
                        updateStatus=MicStatus.NORMAL.getStatus();
                        updateUid=uid;
                    }

                    //开麦条件(闭麦才可以开麦)
                    if(micOperateStatus== MicOperateStatus.OPEN_MIC.getStatus()&&micMetadataValue.getStatus() != MicStatus.CLOSE.getStatus()){
                        throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                    }else{
                        updateStatus=MicStatus.NORMAL.getStatus();
                    }

                    //闭麦条件(只有开麦才可以闭麦)
                    if(micOperateStatus==MicOperateStatus.CLOSE_MIC.getStatus()&&micMetadataValue.getStatus() != MicStatus.NORMAL.getStatus()){
                        throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                    }else{
                        updateStatus=MicStatus.CLOSE.getStatus();
                    }

                    //下麦条件
                    if(micOperateStatus==MicOperateStatus.LEAVE_MIC.getStatus()){
                        throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                    }else{
                        updateStatus=MicStatus.FREE.getStatus();
                        updateUid=null;
                    }

                    //管理员禁言
                    if(micOperateStatus==MicOperateStatus.MUTE_MIC.getStatus() && (!isAdminOperate || StringUtils.isEmpty(micMetadataValue.getUid()) || micMetadataValue.getStatus() != MicStatus.MUTE.getStatus() )){
                        throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                    }else{
                        updateStatus=MicStatus.MUTE.getStatus();
                    }

                    //管理员取消禁言
                    if(micOperateStatus==MicOperateStatus.UNMUTE_MIC.getStatus() && (!isAdminOperate || micMetadataValue.getStatus() != MicStatus.MUTE.getStatus() )){
                        throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                    }else{
                        updateStatus=MicStatus.NORMAL.getStatus();
                    }


                    //更新麦位信息
                    micMetadataValue.setStatus(updateStatus);
                    micMetadataValue.setUid(updateUid);
                    metadata = new HashMap<>();
                    metadata.put(metadataKey, JSONObject.toJSONString(micMetadataValue));
                    imApi.setChatRoomMetadata(OPERATOR, roomId, metadata, AutoDelete.DELETE);
                } else {
                    throw new BaseException(ErrorCodeEnum.mic_not_init);
                }
            }else{
                throw new BaseException(ErrorCodeEnum.mic_is_concurrent_operation);
            }

        }catch (Exception e){
           throw e;
        }finally {
            if(isContinue){
                redisTemplate.delete(redisLockKey);
            }
        }


    }

    private List<MicInfo> buildMicInfo(Map<String, String> metadata) {
        List<MicInfo> micInfos = new ArrayList<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(value, MicMetadataValue.class);

            UserDTO user = null;
            if (!StringUtils.isEmpty(micMetadataValue.getUid())) {
                //查询用户信息
                user=this.userService.getByUid(micMetadataValue.getUid());
            }

            int index = Integer.valueOf(key.split("_")[1]);

            MicInfo micInfo =
                    MicInfo.builder().status(micMetadataValue.getStatus()).index(index).user(user)
                            .build();
            micInfos.add(micInfo);

        }
        micInfos.stream().sorted(Comparator.comparing(mic -> mic.getIndex()));
        return micInfos;
    }

    private String buildMicKey(Integer micIndex){
        String metadataKey = METADATA_PREFIX_KEY+"_" + micIndex;
        return metadataKey;
    }
}
