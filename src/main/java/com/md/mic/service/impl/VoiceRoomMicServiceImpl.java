package com.md.mic.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.easemob.im.server.api.metadata.chatroom.AutoDelete;
import com.easemob.im.server.api.metadata.chatroom.get.ChatRoomMetadataGetResponse;
import com.md.common.im.ImApi;
import com.md.mic.common.constants.CustomEventType;
import com.md.mic.common.constants.MicOperateStatus;
import com.md.mic.common.constants.MicStatus;
import com.md.mic.exception.*;
import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.MicInfo;
import com.md.mic.pojos.MicMetadataValue;
import com.md.mic.pojos.UserDTO;
import com.md.mic.service.UserService;
import com.md.mic.service.VoiceRoomMicService;
import com.md.mic.service.VoiceRoomService;
import com.md.service.common.ErrorCodeEnum;
import com.md.service.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VoiceRoomMicServiceImpl implements VoiceRoomMicService {

    @Autowired
    private ImApi imApi;

    @Autowired
    private UserService userService;

    @Autowired
    private VoiceRoomService voiceRoomService;

    @Resource(name = "voiceRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Resource
    private Redisson redisson;

    private static final String OPERATOR = "admin";

    private static final String METADATA_PREFIX_KEY = "mic";

    @Value("${voice.room.mic.count:9}")
    private int micCount;

    private List<String> allMics = new ArrayList<>();

    @PostConstruct
    public void init() {
        for (int index = 0; index < micCount; index++) {
            allMics.add(buildMicKey(index));
        }
    }

    @Override
    public List<MicInfo> getByRoomId(String roomId) {
        return getRoomMicInfo(voiceRoomService.findByRoomId(roomId).getChatroomId());
    }

    @Override
    public List<MicInfo> getRoomMicInfo(String chatroomId) {
        try {
            ChatRoomMetadataGetResponse chatRoomMetadataGetResponse =
                    imApi.listChatRoomMetadata(chatroomId, allMics);
            Map<String, String> metadata = chatRoomMetadataGetResponse.getMetadata();
            List<MicInfo> micInfo = buildMicInfo(metadata);
            return micInfo;
        } catch (Exception e) {
            log.error("getRoomMicInfo error,roomId:{}", chatroomId, e);
            return Collections.emptyList();
        }

    }

    @Override
    public Boolean setRoomMicInfo(String chatroomId, String uid, Integer micIndex,
            boolean inOrder) {

        boolean hasMic = false;

        if (micIndex == null && !inOrder) {
            throw new MicIndexNullException();
        }
        if (micIndex != null) {
            try {
                this.updateVoiceRoomMicInfo(chatroomId, uid, micIndex,
                        MicOperateStatus.UP_MIC.getStatus(), Boolean.FALSE);
                hasMic = true;
            } catch (Exception e) {
                log.warn("on mic failure,chatroomId:{},uid:{},index:{}", chatroomId, uid, micIndex,
                        e);
            }
        }

        if (!hasMic && inOrder) {

            //按顺序上麦
            for (int index = 1; index < micCount; index++) {
                try {
                    this.updateVoiceRoomMicInfo(chatroomId, uid, index,
                            MicOperateStatus.UP_MIC.getStatus(), Boolean.FALSE);
                    hasMic = true;
                    break;
                } catch (Exception e) {
                    log.warn("on mic failure,roomId:{},uid:{},index:{}", chatroomId, uid, index,
                            e);
                }

            }
        }
        return hasMic;

    }

    @Override
    public List<MicInfo> initMic(String chatroomId, String ownerUid) {

        String redisLockKey = buildMicLockKey(chatroomId);

        RLock micLock = redisson.getLock(redisLockKey);
        boolean lockKey = false;
        try {
            lockKey = micLock.tryLock(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("get redis lock error", e);
        }
        if (!lockKey) {
            throw new MicAlreadyExistsException("mic init already exists");
        }
        try {
            Map<String, String> metadata =
                    imApi.listChatRoomMetadata(chatroomId, Arrays.asList(buildMicKey(0)))
                            .getMetadata();
            if (metadata.size() > 0) {
                throw new MicAlreadyExistsException("mic init already exists");
            }

            Map<String, String> metadataMap = new HashMap<>();
            for (int micIndex = 0; micIndex < micCount; micIndex++) {
                String micKey = buildMicKey(micIndex);
                MicMetadataValue micMetadataValue;
                if (micIndex == 0) {
                    micMetadataValue = new MicMetadataValue(ownerUid, MicStatus.NORMAL.getStatus());
                } else {
                    micMetadataValue = new MicMetadataValue(null, MicStatus.FREE.getStatus());
                }
                metadataMap.put(micKey, JSON.toJSONString(micMetadataValue));
            }
            //
            List<String> successKeys =
                    imApi.setChatRoomMetadata(OPERATOR, chatroomId, metadataMap,
                                    AutoDelete.DELETE)
                            .getSuccessKeys();
            if (successKeys.size() != micCount) {
                imApi.deleteChatRoomMetadata(OPERATOR, chatroomId, successKeys);
                throw new MicInitException();
            }
            return buildMicInfo(metadataMap);
        } catch (Exception e) {
            log.error("init mic to easemob failed | roomId={}, err=", chatroomId, e);
            throw e;
        } finally {
            micLock.unlock();
        }
    }

    @Override
    public void closeMic(String uid, String chatroomId, Integer micIndex) {
        String metadataKey = buildMicKey(micIndex);
        Map<String, String> metadata =
                imApi.listChatRoomMetadata(chatroomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid()) || !micMetadataValue.getUid()
                    .equals(uid)) {
                throw new MicNotBelongYouException();
            }
            if (micMetadataValue.getStatus() == MicStatus.NORMAL.getStatus()) {
                this.updateVoiceRoomMicInfo(chatroomId, uid, micIndex,
                        MicOperateStatus.CLOSE_MIC.getStatus(), Boolean.FALSE);
            } else {
                throw new MicStatusCannotBeModifiedException();
            }

        } else {
            throw new MicInitException();
        }
    }

    @Override
    public void openMic(String uid, String chatroomId, Integer micIndex) {

        String metadataKey = buildMicKey(micIndex);

        Map<String, String> metadata =
                imApi.listChatRoomMetadata(chatroomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid()) || !micMetadataValue.getUid()
                    .equals(uid)) {
                throw new MicNotBelongYouException();
            }
            if (micMetadataValue.getStatus() == MicStatus.CLOSE.getStatus()) {
                this.updateVoiceRoomMicInfo(chatroomId, uid, micIndex,
                        MicOperateStatus.OPEN_MIC.getStatus(), Boolean.FALSE);
            } else {
                throw new MicStatusCannotBeModifiedException();
            }

        } else {
            throw new MicInitException();
        }
    }

    @Override
    public void leaveMic(String uid, String chatroomId, Integer micIndex) {
        String metadataKey = buildMicKey(micIndex);
        Map<String, String> metadata =
                imApi.listChatRoomMetadata(chatroomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid()) || !micMetadataValue.getUid()
                    .equals(uid)) {
                throw new MicNotBelongYouException();
            }

            this.updateVoiceRoomMicInfo(chatroomId, uid, micIndex,
                    MicOperateStatus.LEAVE_MIC.getStatus(), Boolean.FALSE);

        } else {
            throw new MicInitException();
        }
    }

    @Override
    public void muteMic(String chatroomId, Integer micIndex) {

        String metadataKey = buildMicKey(micIndex);
        Map<String, String> metadata =
                imApi.listChatRoomMetadata(chatroomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid())
                    || micMetadataValue.getStatus() == MicStatus.MUTE.getStatus()) {
                throw new MicStatusCannotBeModifiedException();
            }

            this.updateVoiceRoomMicInfo(chatroomId, null, micIndex,
                    MicOperateStatus.MUTE_MIC.getStatus(), Boolean.TRUE);

        } else {
            throw new MicInitException();
        }
    }

    @Override
    public void unMuteMic(String chatroomId, Integer micIndex) {
        String metadataKey = buildMicKey(micIndex);
        Map<String, String> metadata =
                imApi.listChatRoomMetadata(chatroomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid())
                    || micMetadataValue.getStatus() != MicStatus.MUTE.getStatus()) {
                throw new MicStatusCannotBeModifiedException();
            }

            this.updateVoiceRoomMicInfo(chatroomId, null, micIndex,
                    MicOperateStatus.UNMUTE_MIC.getStatus(), Boolean.TRUE);

        } else {
            throw new MicInitException();
        }
    }

    @Override
    public void kickUserMic(String chatroomId, Integer micIndex, String uid) {

        String metadataKey = buildMicKey(micIndex);

        Map<String, String> metadata =
                imApi.listChatRoomMetadata(chatroomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid())) {
                throw new MicStatusCannotBeModifiedException();
            }

            if (!micMetadataValue.getUid().equals(uid)) {
                throw new MicNotCurrentUserException();
            }

            this.updateVoiceRoomMicInfo(chatroomId, null, micIndex,
                    MicOperateStatus.KICK_MIC.getStatus(), Boolean.TRUE);

        } else {
            throw new MicInitException();
        }
    }

    @Override
    public void lockMic(String chatroomId, Integer micIndex) {

        String metadataKey = buildMicKey(micIndex);

        Map<String, String> metadata =
                imApi.listChatRoomMetadata(chatroomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (micMetadataValue.getStatus() != MicStatus.FREE.getStatus()) {
                throw new MicStatusCannotBeModifiedException();
            }

            this.updateVoiceRoomMicInfo(chatroomId, null, micIndex,
                    MicOperateStatus.LOCK_MIC.getStatus(), Boolean.TRUE);

        } else {
            throw new MicInitException();
        }
    }

    @Override
    public void unLockMic(String chatroomId, Integer micIndex) {

        String metadataKey = buildMicKey(micIndex);

        Map<String, String> metadata =
                imApi.listChatRoomMetadata(chatroomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (micMetadataValue.getStatus() != MicStatus.LOCK.getStatus()) {
                throw new MicStatusCannotBeModifiedException();
            }

            this.updateVoiceRoomMicInfo(chatroomId, null, micIndex,
                    MicOperateStatus.UNLOCK_MIC.getStatus(), Boolean.TRUE);

        } else {
            throw new MicInitException();
        }
    }

    @Override
    public void invite(VoiceRoom roomInfo, Integer index, String uid) {
        UserDTO userDTO = this.userService.getByUid(uid);
        if (userDTO == null) {
            throw new BaseException(ErrorCodeEnum.user_not_exist);
        }
        UserDTO applyUser = userService.getByUid(uid);

        Map<String, Object> customExtensions = new HashMap<>();
        customExtensions.put("user", JSONObject.toJSONString(applyUser));
        customExtensions.put("mic_index", index.toString());
        customExtensions.put("room_id", roomInfo.getRoomId());
        this.imApi.sendUserCustomMessage(roomInfo.getOwner(), applyUser.getChatUid(),
                CustomEventType.INVITE_SITE.getValue(), customExtensions, new HashMap<>());
    }

    @Override
    public Boolean agreeInvite(String chatroomId, String uid, Integer micIndex) {
        if (micIndex == null) {
            return setRoomMicInfo(chatroomId, uid, null, Boolean.TRUE);
        } else {
            return setRoomMicInfo(chatroomId, uid, micIndex, Boolean.FALSE);
        }

    }

    @Override
    public Boolean refuseInvite(VoiceRoom roomInfo, String uid) {

        UserDTO userDTO = this.userService.getByUid(uid);

        if (userDTO == null) {
            throw new BaseException(ErrorCodeEnum.user_not_exist);
        }
        UserDTO applyUser = userService.getByUid(uid);

        Map<String, Object> customExtensions = new HashMap<>();
        customExtensions.put("user", applyUser);
        customExtensions.put("room_id", roomInfo.getRoomId());
        this.imApi.sendUserCustomMessage(userDTO.getChatUid(), roomInfo.getOwner(),
                CustomEventType.INVITE_REFUSED.getValue(), customExtensions, new HashMap<>());

        return Boolean.TRUE;

    }

    @Override
    public void exchangeMic(String chatroomId, Integer from, Integer to, String uid) {

        if(from==0){
            throw new MicStatusCannotBeModifiedException();
        }

        String fromMicKey = buildMicKey(from);
        String toMicKey = buildMicKey(to);

        Map<String, String> metadata =
                imApi.listChatRoomMetadata(chatroomId, Arrays.asList(fromMicKey, toMicKey))
                        .getMetadata();

        if (metadata.containsKey(fromMicKey) && metadata.containsKey(toMicKey)) {
            MicMetadataValue fromMicMetadataValue =
                    JSONObject.parseObject(metadata.get(fromMicKey), MicMetadataValue.class);

            MicMetadataValue toMicMetadataValue =
                    JSONObject.parseObject(metadata.get(toMicKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(fromMicMetadataValue.getUid()) || !fromMicMetadataValue.getUid()
                    .equals(uid)) {
                throw new MicNotBelongYouException();
            }

            if (toMicMetadataValue.getStatus() != MicStatus.FREE.getStatus()) {
                throw new MicStatusCannotBeModifiedException();
            }

            this.exchangeMicInfo(chatroomId, uid, from, to);

        } else {
            throw new MicInitException();
        }

    }

    private void exchangeMicInfo(String chatroomId, String uid, Integer from, Integer to) {

        String fromMicKey = buildMicKey(from);

        String toMicKey = buildMicKey(to);
        //todo 使用 redisson
        Boolean lockFromkey = redisTemplate.opsForValue()
                .setIfAbsent(buildMicLockKey(from), fromMicKey, Duration.ofMillis(5000));

        Boolean lockTokey = redisTemplate.opsForValue()
                .setIfAbsent(buildMicLockKey(to), toMicKey, Duration.ofMillis(5000));

        try {
            if (lockFromkey && lockTokey) {

                Map<String, String> metadata =
                        imApi.listChatRoomMetadata(chatroomId, Arrays.asList(fromMicKey, toMicKey))
                                .getMetadata();

                if (metadata.containsKey(fromMicKey) && metadata.containsKey(toMicKey)) {
                    //TODO 不要使用fastjson
                    MicMetadataValue fromMicMetadataValue =
                            JSONObject
                                    .parseObject(metadata.get(fromMicKey), MicMetadataValue.class);

                    MicMetadataValue toMicMetadataValue =
                            JSONObject.parseObject(metadata.get(toMicKey), MicMetadataValue.class);

                    if (StringUtils.isEmpty(fromMicMetadataValue.getUid()) || !fromMicMetadataValue
                            .getUid().equals(uid)) {
                        throw new MicNotBelongYouException();
                    }

                    if (toMicMetadataValue.getStatus() != MicStatus.FREE.getStatus()) {
                        throw new MicStatusCannotBeModifiedException();
                    }

                    int currentStatus = fromMicMetadataValue.getStatus();

                    fromMicMetadataValue = new MicMetadataValue(null, MicStatus.FREE.getStatus());
                    toMicMetadataValue = new MicMetadataValue(uid, currentStatus);

                    metadata = new HashMap<>();
                    metadata.put(fromMicKey, JSONObject.toJSONString(fromMicMetadataValue));
                    metadata.put(toMicKey, JSONObject.toJSONString(toMicMetadataValue));
                    imApi.setChatRoomMetadata(OPERATOR, chatroomId, metadata, AutoDelete.DELETE);

                } else {
                    throw new MicInitException();
                }
            } else {
                throw new BaseException(ErrorCodeEnum.mic_is_concurrent_operation);
            }
        } catch (Exception e) {
            log.error("exchangeMicInfo error,roomId:{},from:{},to:{},uid:{}", chatroomId,
                    from, to, uid, e);
            throw e;
        } finally {
            if (lockFromkey) {
                redisTemplate.delete(buildMicLockKey(from));
            }
            if (lockTokey) {
                redisTemplate.delete(buildMicLockKey(to));
            }
        }

    }

    private void updateVoiceRoomMicInfo(String chatroomId, String uid, Integer micIndex,
            Integer micOperateStatus, boolean isAdminOperate) { //todo isAdminOperate 这个换成 boolean类型有没有问题
        String metadataKey = buildMicKey(micIndex);
        String redisLockKey = buildMicLockKey(micIndex);
        //todo 统一使用方式
        Boolean isContinue = redisTemplate.opsForValue()
                .setIfAbsent(redisLockKey, metadataKey, Duration.ofMillis(5000));
        try {
            if (isContinue) {
                Map<String, String> metadata =
                        imApi.listChatRoomMetadata(chatroomId, Arrays.asList(metadataKey))
                                .getMetadata();
                if (metadata.containsKey(metadataKey)) {

                    MicMetadataValue micMetadataValue =
                            JSONObject
                                    .parseObject(metadata.get(metadataKey), MicMetadataValue.class);

                    Integer updateStatus = null;
                    String updateUid = micMetadataValue.getUid();

                    //麦有人的情况下、只允许对该人进行操作 todo 去掉这种注释
                    if (!isAdminOperate && !StringUtils.isEmpty(micMetadataValue.getUid())
                            && !micMetadataValue.getUid()
                            .equals(uid)) {
                        throw new MicNotBelongYouException();
                    }

                    switch (MicOperateStatus.parse(micOperateStatus)) {
                        case UP_MIC:
                            if (micMetadataValue.getStatus() == MicStatus.FREE.getStatus()) {
                                updateStatus = MicStatus.NORMAL.getStatus();
                                updateUid = uid;
                            } else {
                                throw new MicStatusCannotBeModifiedException();
                            }
                            break;
                        case OPEN_MIC:
                            if (micMetadataValue.getStatus() == MicStatus.CLOSE.getStatus()) {
                                updateStatus = MicStatus.NORMAL.getStatus();
                                updateUid = uid;
                            } else {
                                throw new MicStatusCannotBeModifiedException();
                            }
                            break;
                        case CLOSE_MIC:
                            if (micMetadataValue.getStatus() == MicStatus.NORMAL.getStatus()) {
                                updateStatus = MicStatus.CLOSE.getStatus();
                                updateUid = uid;
                            } else {
                                throw new MicStatusCannotBeModifiedException();
                            }
                            break;
                        case LEAVE_MIC:
                            if(micIndex==0){
                                throw new MicStatusCannotBeModifiedException();
                            }
                            updateStatus = MicStatus.FREE.getStatus();
                            updateUid = null;
                            break;
                        case MUTE_MIC:
                            if(micIndex==0){
                                throw new MicStatusCannotBeModifiedException();
                            }
                            if (isAdminOperate && !StringUtils.isEmpty(micMetadataValue.getUid())
                                    && micMetadataValue.getStatus() != MicStatus.MUTE.getStatus()) {
                                updateStatus = MicStatus.MUTE.getStatus();
                            } else {
                                throw new MicStatusCannotBeModifiedException();
                            }
                            break;
                        case UNMUTE_MIC:
                            if (isAdminOperate && micMetadataValue.getStatus() == MicStatus.MUTE
                                    .getStatus()) {
                                updateStatus = MicStatus.NORMAL.getStatus();
                            } else {
                                throw new MicStatusCannotBeModifiedException();
                            }
                            break;
                        case LOCK_MIC:
                            if (isAdminOperate && micMetadataValue.getStatus() == MicStatus.FREE
                                    .getStatus()) {
                                updateStatus = MicStatus.LOCK.getStatus();
                                updateUid = null;
                            } else {
                                throw new MicStatusCannotBeModifiedException();
                            }
                            break;
                        case UNLOCK_MIC:
                            if (isAdminOperate && micMetadataValue.getStatus() == MicStatus.LOCK
                                    .getStatus()) {
                                updateStatus = MicStatus.FREE.getStatus();
                                updateUid = null;
                            } else {
                                throw new MicStatusCannotBeModifiedException();
                            }
                            break;
                        case KICK_MIC:
                            if (isAdminOperate && !StringUtils.isEmpty(micMetadataValue.getUid())) {
                                updateStatus = MicStatus.FREE.getStatus();
                                updateUid = null;
                            } else {
                                throw new MicStatusCannotBeModifiedException();
                            }
                            break;
                        default:
                            break;
                    }

                    //更新麦位信息
                    micMetadataValue = new MicMetadataValue(updateUid, updateStatus);
                    metadata = new HashMap<>();
                    metadata.put(metadataKey, JSONObject.toJSONString(micMetadataValue));
                    imApi.setChatRoomMetadata(OPERATOR, chatroomId, metadata, AutoDelete.DELETE);
                } else {
                    throw new MicInitException();
                }
            } else {
                throw new BaseException(ErrorCodeEnum.mic_is_concurrent_operation);
            }

        } catch (Exception e) {
            // todo 没有任何意义的catch 不要有
            throw e;
        } finally {
            if (isContinue) {
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
                user = this.userService.getByUid(micMetadataValue.getUid());
            }
            //todo 会不会有空指针或者数组越界的问题
            int index = Integer.parseInt(key.split("_")[1]);

            MicInfo micInfo =
                    MicInfo.builder().status(micMetadataValue.getStatus()).index(index).user(user)
                            .build();
            micInfos.add(micInfo);

        }
        micInfos = micInfos.stream().sorted(Comparator.comparing(MicInfo::getIndex)).collect(
                Collectors.toList());
        return micInfos;
    }

    private String buildMicKey(Integer micIndex) {
        return METADATA_PREFIX_KEY + "_" + micIndex;
    }

    private String buildMicLockKey(Integer micIndex) {
        return buildMicKey(micIndex) + "_lock";
    }

    private String buildMicLockKey(String roomId) {
        return roomId + "_lock";
    }
}
