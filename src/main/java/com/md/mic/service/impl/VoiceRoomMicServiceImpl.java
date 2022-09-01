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

    private List<String> allMics = new ArrayList<>();

    @PostConstruct
    public void init() {
        for (int index = 0; index < micCount; index++) {
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
    public Boolean setRoomMicInfo(String roomId, String uid, Integer micIndex, Boolean inOrder) {

        Boolean hasMic = false;

        if (micIndex == null && !inOrder) {
            throw new BaseException(ErrorCodeEnum.mic_index_is_not_null);
        }
        if (micIndex != null) {
            try {
                this.updateVoiceRoomMicInfo(roomId, uid, micIndex,
                        MicOperateStatus.UP_MIC.getStatus(), Boolean.FALSE);
                hasMic = true;
            } catch (Exception e) {
                log.warn("on mic failure,roomId:{},uid:{},index:{}", roomId, uid, micIndex,
                        e);
            }
        }

        if (!hasMic && inOrder) {

            //按顺序上麦
            for (int index = 1; index < micCount; index++) {
                try {
                    this.updateVoiceRoomMicInfo(roomId, uid, index,
                            MicOperateStatus.UP_MIC.getStatus(), Boolean.FALSE);
                    hasMic = true;
                    break;
                } catch (Exception e) {
                    log.warn("on mic failure,roomId:{},uid:{},index:{}", roomId, uid, index,
                            e);
                }

            }
        }
        return hasMic;

    }

    @Override
    public void initMic(VoiceRoomDTO voiceRoomDTO) {
        String roomId = voiceRoomDTO.getRoomId();
        String owner = voiceRoomDTO.getOwner().getUid();
        Boolean lockkey = redisTemplate.opsForValue()
                .setIfAbsent(buildMicLockKey(roomId), buildMicLockKey(roomId),
                        Duration.ofMillis(5000));

        try {
            if (lockkey) {

                Map<String, String> metadata =
                        imApi.listChatRoomMetadata(roomId, Arrays.asList(buildMicKey(0)))
                                .getMetadata();
                if (metadata.size() > 0) {
                    throw new BaseException(ErrorCodeEnum.mic_init_already);
                }

                Map<String, String> metadataMap = new HashMap<>();
                for (int micIndex = 0; micIndex < micCount; micIndex++) {
                    String micKey = buildMicKey(micIndex);
                    MicMetadataValue micMetadataValue = new MicMetadataValue();
                    micMetadataValue.setUid(null);
                    micMetadataValue.setStatus(MicStatus.FREE.getStatus());
                    if (micIndex == 0) {
                        micMetadataValue.setUid(owner);
                    }
                    metadataMap.put(micKey, JSONObject.toJSONString(micMetadataValue));
                }
                //
                List<String> successKeys =
                        imApi.setChatRoomMetadata(OPERATOR, roomId, metadataMap, AutoDelete.DELETE)
                                .getSuccessKeys();
                if (successKeys.size() != micCount) {
                    imApi.deleteChatRoomMetadata(OPERATOR, roomId, successKeys);
                    throw new BaseException(ErrorCodeEnum.mic_init_error);
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (lockkey) {
                redisTemplate.delete(lockkey);
            }
        }
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
            if (micMetadataValue.getStatus() == MicStatus.NORMAL.getStatus()) {
                this.updateVoiceRoomMicInfo(roomId, uid, micIndex,
                        MicOperateStatus.CLOSE_MIC.getStatus(), Boolean.FALSE);
            } else {
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
            if (micMetadataValue.getStatus() == MicStatus.CLOSE.getStatus()) {
                this.updateVoiceRoomMicInfo(roomId, uid, micIndex,
                        MicOperateStatus.OPEN_MIC.getStatus(), Boolean.FALSE);
            } else {
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

            this.updateVoiceRoomMicInfo(roomId, uid, micIndex,
                    MicOperateStatus.LEAVE_MIC.getStatus(), Boolean.FALSE);

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

            if (StringUtils.isEmpty(micMetadataValue.getUid())
                    || micMetadataValue.getStatus() == MicStatus.MUTE.getStatus()) {
                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
            }

            this.updateVoiceRoomMicInfo(roomId, null, micIndex,
                    MicOperateStatus.MUTE_MIC.getStatus(), Boolean.TRUE);

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

            if (StringUtils.isEmpty(micMetadataValue.getUid())
                    || micMetadataValue.getStatus() != MicStatus.MUTE.getStatus()) {
                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
            }

            this.updateVoiceRoomMicInfo(roomId, null, micIndex,
                    MicOperateStatus.UNMUTE_MIC.getStatus(), Boolean.TRUE);

        } else {
            throw new BaseException(ErrorCodeEnum.mic_not_init);
        }
    }

    @Override
    public void kickUserMic(String roomId, Integer micIndex, String uid) {

        String metadataKey = buildMicKey(micIndex);

        Map<String, String> metadata =
                imApi.listChatRoomMetadata(roomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(micMetadataValue.getUid())) {
                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
            }

            if (!micMetadataValue.getUid().equals(uid)) {
                throw new BaseException(ErrorCodeEnum.mic_not_current_user);
            }

            this.updateVoiceRoomMicInfo(roomId, null, micIndex,
                    MicOperateStatus.KICK_MIC.getStatus(), Boolean.TRUE);

        } else {
            throw new BaseException(ErrorCodeEnum.mic_not_init);
        }
    }

    @Override
    public void lockMic(String roomId, Integer micIndex) {

        String metadataKey = buildMicKey(micIndex);

        Map<String, String> metadata =
                imApi.listChatRoomMetadata(roomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (micMetadataValue.getStatus() != MicStatus.FREE.getStatus()) {
                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
            }

            this.updateVoiceRoomMicInfo(roomId, null, micIndex,
                    MicOperateStatus.LOCK_MIC.getStatus(), Boolean.TRUE);

        } else {
            throw new BaseException(ErrorCodeEnum.mic_not_init);
        }
    }

    @Override
    public void unLockMic(String roomId, Integer micIndex) {

        String metadataKey = buildMicKey(micIndex);

        Map<String, String> metadata =
                imApi.listChatRoomMetadata(roomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {
            MicMetadataValue micMetadataValue =
                    JSONObject.parseObject(metadata.get(metadataKey), MicMetadataValue.class);

            if (micMetadataValue.getStatus() != MicStatus.LOCK.getStatus()) {
                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
            }

            this.updateVoiceRoomMicInfo(roomId, null, micIndex,
                    MicOperateStatus.UNLOCK_MIC.getStatus(), Boolean.TRUE);

        } else {
            throw new BaseException(ErrorCodeEnum.mic_not_init);
        }
    }

    @Override
    public void invite(VoiceRoomDTO roomInfo, Integer index, String uid) {
        UserDTO userDTO = this.userService.getByUid(uid);
        if (userDTO == null) {
            throw new BaseException(ErrorCodeEnum.user_not_exist);
        }
        UserDTO applyUser = userService.getByUid(uid);

        Map<String, Object> customExtensions = new HashMap<>();
        customExtensions.put("user", applyUser);
        customExtensions.put("mic_index", index);
        customExtensions.put("room_id", roomInfo.getRoomId());
        this.imApi.sendUserCustomMessage(roomInfo.getOwner().getChatUid(), applyUser.getChatUid(),
                CustomEventType.INVITE_SITE.getValue(), customExtensions, new HashMap<>());
    }

    @Override
    public Boolean agreeInvite(String roomId, String uid, Integer micIndex) {
        if (micIndex == null) {
            return setRoomMicInfo(roomId, uid, null, Boolean.TRUE);
        } else {
            return setRoomMicInfo(roomId, uid, micIndex, Boolean.FALSE);
        }

    }

    @Override
    public void exchangeMic(String roomId, Integer from, Integer to, String uid) {
        String fromMicKey = buildMicKey(from);
        String toMicKey = buildMicKey(to);

        Map<String, String> metadata =
                imApi.listChatRoomMetadata(roomId, Arrays.asList(fromMicKey, toMicKey))
                        .getMetadata();

        if (metadata.containsKey(fromMicKey) && metadata.containsKey(toMicKey)) {
            MicMetadataValue fromMicMetadataValue =
                    JSONObject.parseObject(metadata.get(fromMicKey), MicMetadataValue.class);

            MicMetadataValue toMicMetadataValue =
                    JSONObject.parseObject(metadata.get(toMicKey), MicMetadataValue.class);

            if (StringUtils.isEmpty(fromMicMetadataValue.getUid()) || !fromMicMetadataValue.getUid()
                    .equals(uid)) {
                throw new BaseException(ErrorCodeEnum.mic_not_belong_you);
            }

            if (toMicMetadataValue.getStatus() != MicStatus.FREE.getStatus()) {
                throw new BaseException(ErrorCodeEnum.mic_index_is_not_free);
            }

            this.exchangeMicInfo(roomId, uid, from, to);

        } else {
            throw new BaseException(ErrorCodeEnum.mic_not_init);
        }

    }

    private void exchangeMicInfo(String roomId, String uid, Integer from, Integer to) {

        String fromMicKey = buildMicKey(from);

        String toMicKey = buildMicKey(to);

        Boolean lockFromkey = redisTemplate.opsForValue()
                .setIfAbsent(buildMicLockKey(from), fromMicKey, Duration.ofMillis(5000));

        Boolean lockTokey = redisTemplate.opsForValue()
                .setIfAbsent(buildMicLockKey(to), toMicKey, Duration.ofMillis(5000));

        try {
            if (lockFromkey && lockTokey) {

                Map<String, String> metadata =
                        imApi.listChatRoomMetadata(roomId, Arrays.asList(fromMicKey, toMicKey))
                                .getMetadata();

                if (metadata.containsKey(fromMicKey) && metadata.containsKey(toMicKey)) {

                    MicMetadataValue fromMicMetadataValue =
                            JSONObject
                                    .parseObject(metadata.get(fromMicKey), MicMetadataValue.class);

                    MicMetadataValue toMicMetadataValue =
                            JSONObject.parseObject(metadata.get(toMicKey), MicMetadataValue.class);

                    if (StringUtils.isEmpty(fromMicMetadataValue.getUid()) || !fromMicMetadataValue
                            .getUid().equals(uid)) {
                        throw new BaseException(ErrorCodeEnum.mic_not_belong_you);
                    }

                    if (toMicMetadataValue.getStatus() != MicStatus.FREE.getStatus()) {
                        throw new BaseException(ErrorCodeEnum.mic_index_is_not_free);
                    }

                    int currentStatus = fromMicMetadataValue.getStatus();

                    fromMicMetadataValue.setUid(null);
                    fromMicMetadataValue.setStatus(MicStatus.FREE.getStatus());
                    toMicMetadataValue.setUid(uid);
                    toMicMetadataValue.setStatus(currentStatus);

                    metadata = new HashMap<>();
                    metadata.put(fromMicKey, JSONObject.toJSONString(fromMicMetadataValue));
                    metadata.put(toMicKey, JSONObject.toJSONString(toMicMetadataValue));
                    imApi.setChatRoomMetadata(OPERATOR, roomId, metadata, AutoDelete.DELETE);

                } else {
                    throw new BaseException(ErrorCodeEnum.mic_not_init);
                }
            } else {
                throw new BaseException(ErrorCodeEnum.mic_is_concurrent_operation);
            }
        } catch (Exception e) {
            log.error("exchangeMicInfo error,roomId:{},from:{},to:{},uid:{}", roomId, from, to, uid,
                    e);
            throw e;
        } finally {
            if (lockFromkey) {
                redisTemplate.delete(lockFromkey);
            }
            if (lockTokey) {
                redisTemplate.delete(lockTokey);
            }
        }

    }

    private void updateVoiceRoomMicInfo(String roomId, String uid, Integer micIndex,
            Integer micOperateStatus, Boolean isAdminOperate) {
        String metadataKey = buildMicKey(micIndex);
        String redisLockKey = buildMicLockKey(micIndex);
        Boolean isContinue = redisTemplate.opsForValue()
                .setIfAbsent(redisLockKey, metadataKey, Duration.ofMillis(5000));
        try {
            if (isContinue) {
                Map<String, String> metadata =
                        imApi.listChatRoomMetadata(roomId, Arrays.asList(metadataKey))
                                .getMetadata();
                if (metadata.containsKey(metadataKey)) {

                    MicMetadataValue micMetadataValue =
                            JSONObject
                                    .parseObject(metadata.get(metadataKey), MicMetadataValue.class);

                    Integer updateStatus = null;
                    String updateUid = micMetadataValue.getUid();

                    //麦有人的情况下、只允许对该人进行操作
                    if (!isAdminOperate && !StringUtils.isEmpty(micMetadataValue.getUid())
                            && !micMetadataValue.getUid()
                            .equals(uid)) {
                        throw new BaseException(ErrorCodeEnum.mic_not_belong_you);
                    }

                    switch (MicOperateStatus.parse(micOperateStatus)) {
                        case UP_MIC:
                            if (micMetadataValue.getStatus() == MicStatus.FREE.getStatus()) {
                                updateStatus = MicStatus.NORMAL.getStatus();
                                updateUid = uid;
                            } else {
                                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                            }
                            break;
                        case OPEN_MIC:
                            if (micMetadataValue.getStatus() == MicStatus.CLOSE.getStatus()) {
                                updateStatus = MicStatus.NORMAL.getStatus();
                                updateUid = uid;
                            } else {
                                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                            }
                            break;
                        case CLOSE_MIC:
                            if (micMetadataValue.getStatus() == MicStatus.NORMAL.getStatus()) {
                                updateStatus = MicStatus.CLOSE.getStatus();
                                updateUid = uid;
                            } else {
                                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                            }
                            break;
                        case LEAVE_MIC:
                            updateStatus = MicStatus.FREE.getStatus();
                            updateUid = null;
                            break;
                        case MUTE_MIC:
                            if (isAdminOperate && !StringUtils.isEmpty(micMetadataValue.getUid())
                                    && micMetadataValue.getStatus() != MicStatus.MUTE.getStatus()) {
                                updateStatus = MicStatus.MUTE.getStatus();
                                updateUid = uid;
                            } else {
                                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                            }
                            break;
                        case UNMUTE_MIC:
                            if (isAdminOperate && micMetadataValue.getStatus() == MicStatus.MUTE
                                    .getStatus()) {
                                updateStatus = MicStatus.NORMAL.getStatus();
                                updateUid = uid;
                            } else {
                                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                            }
                            break;
                        case LOCK_MIC:
                            if (isAdminOperate && micMetadataValue.getStatus() == MicStatus.FREE
                                    .getStatus()) {
                                updateStatus = MicStatus.LOCK.getStatus();
                                updateUid = null;
                            } else {
                                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                            }
                            break;
                        case UNLOCK_MIC:
                            if (isAdminOperate && micMetadataValue.getStatus() == MicStatus.LOCK
                                    .getStatus()) {
                                updateStatus = MicStatus.FREE.getStatus();
                                updateUid = null;
                            } else {
                                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                            }
                            break;
                        case KICK_MIC:
                            if (isAdminOperate && !StringUtils.isEmpty(micMetadataValue.getUid())) {
                                updateStatus = MicStatus.FREE.getStatus();
                                updateUid = null;
                            } else {
                                throw new BaseException(ErrorCodeEnum.mic_is_cannot_be_modified);
                            }
                            break;
                        default:
                            break;
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
            } else {
                throw new BaseException(ErrorCodeEnum.mic_is_concurrent_operation);
            }

        } catch (Exception e) {
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

            int index = Integer.valueOf(key.split("_")[1]);

            MicInfo micInfo =
                    MicInfo.builder().status(micMetadataValue.getStatus()).index(index).user(user)
                            .build();
            micInfos.add(micInfo);

        }
        micInfos.stream().sorted(Comparator.comparing(mic -> mic.getIndex()));
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
