package com.md.mic.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.easemob.im.server.api.metadata.chatroom.AutoDelete;
import com.easemob.im.server.api.metadata.chatroom.get.ChatRoomMetadataGetResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.md.mic.service.VoiceRoomUserService;
import com.md.service.common.ErrorCodeEnum;
import com.md.service.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VoiceRoomMicServiceImpl implements VoiceRoomMicService {

    private static final String OPERATOR = "admin";

    private static final String METADATA_PREFIX_KEY = "mic";

    @Resource
    private ImApi imApi;

    @Resource
    private UserService userService;

    @Resource
    private VoiceRoomService voiceRoomService;

    @Resource
    private VoiceRoomUserService voiceRoomUserService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource(name = "voiceRoomRedisson")
    private RedissonClient redisson;

    @Override
    public List<MicInfo> getByRoomId(String roomId) {
        return getRoomMicInfo(voiceRoomService.findByRoomId(roomId));
    }

    @Override
    public List<MicInfo> getRoomMicInfo(VoiceRoom voiceRoom) {

        String chatroomId = voiceRoom.getChatroomId();
        int micCount = voiceRoom.getMicCount() + voiceRoom.getRobotCount();
        List<String> allMics = new ArrayList<>();
        for (int index = 0; index < micCount; index++) {
            allMics.add(buildMicKey(index));
        }
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
    public Boolean setRoomMicInfo(VoiceRoom roomInfo, String uid, Integer micIndex,
            boolean inOrder) {

        boolean hasMic = false;

        if (micIndex == null && !inOrder) {
            throw new MicIndexNullException();
        }
        List<MicInfo> micInfos = this.getRoomMicInfo(roomInfo);
        Optional<MicInfo> micInfo = micInfos.stream().filter((mic) -> mic.getMember() != null
                && mic.getMember().getUid().equals(uid)).findFirst();
        String chatroomId = roomInfo.getChatroomId();
        if (micInfo.isPresent()) {
            throw new MicAlreadyExistsException("mic user already exists");
        }

        Integer micCount = roomInfo.getMicCount();

        if (micIndex != null) {
            if (micIndex >= micCount) {
                throw new MicIndexExceedLimitException("mic index exceed the maximum");
            }
            if (0 > micIndex) {
                throw new MicIndexExceedLimitException("mic index exceed the minimum");
            }
            try {
                this.updateVoiceRoomMicInfo(chatroomId, uid, micIndex,
                        MicOperateStatus.UP_MIC.getStatus(), Boolean.FALSE, roomInfo.getRoomId());
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
                            MicOperateStatus.UP_MIC.getStatus(), Boolean.FALSE,
                            roomInfo.getRoomId());
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
    public List<MicInfo> initMic(VoiceRoom voiceRoom, Boolean isActive) {

        int micCount = voiceRoom.getMicCount() + voiceRoom.getRobotCount();

        String chatroomId = voiceRoom.getChatroomId();

        String ownerUid = voiceRoom.getOwner();

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
                if ((micIndex + voiceRoom.getRobotCount()) >= micCount) {
                    if (isActive) {
                        micMetadataValue = new MicMetadataValue(null, MicStatus.ACTIVE.getStatus());
                    } else {
                        micMetadataValue =
                                new MicMetadataValue(null, MicStatus.INACTIVE.getStatus());
                    }
                }

                String jsonValue = "";
                try {
                    jsonValue = objectMapper.writeValueAsString(micMetadataValue);
                } catch (Exception e) {
                    log.error("write MicMetadataValue json failed | MicMetadataValue={}, err=",
                            micMetadataValue, e);
                }
                metadataMap.put(micKey, jsonValue);
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
        }
    }

    @Override
    public void updateRobotMicStatus(VoiceRoom voiceRoom, Boolean isActive) {
        Integer robotCount = voiceRoom.getRobotCount();
        Map<String, String> metadata = new HashMap<>();
        for (int index = 0; index < robotCount; index++) {
            String robotMetaDataKey = buildMicKey(voiceRoom.getMicCount() + index);
            MicMetadataValue micMetadataValue = new MicMetadataValue(null,
                    isActive ? MicStatus.ACTIVE.getStatus() : MicStatus.INACTIVE.getStatus());
            String jsonValue = "";
            try {
                jsonValue = objectMapper.writeValueAsString(micMetadataValue);
            } catch (Exception e) {
                log.error("write MicMetadataValue json failed | MicMetadataValue={}, err=",
                        micMetadataValue, e);
            }
            metadata.put(robotMetaDataKey, jsonValue);
        }
        imApi.setChatRoomMetadata(OPERATOR, voiceRoom.getChatroomId(), metadata,
                AutoDelete.DELETE)
                .getSuccessKeys();
    }

    @Override
    public void closeMic(String uid, String chatroomId, Integer micIndex, String roomId) {

        MicMetadataValue micMetadataValue = buildMicMetadataValue(chatroomId, micIndex);

        if (StringUtils.isEmpty(micMetadataValue.getUid()) || !micMetadataValue.getUid()
                .equals(uid)) {
            throw new MicNotBelongYouException();
        }
        if (micMetadataValue.getStatus() == MicStatus.NORMAL.getStatus()) {
            this.updateVoiceRoomMicInfo(chatroomId, uid, micIndex,
                    MicOperateStatus.CLOSE_MIC.getStatus(), Boolean.FALSE, roomId);
        } else {
            throw new MicStatusCannotBeModifiedException();
        }

    }

    @Override
    public void openMic(String uid, String chatroomId, Integer micIndex, String roomId) {

        MicMetadataValue micMetadataValue = buildMicMetadataValue(chatroomId, micIndex);

        if (StringUtils.isEmpty(micMetadataValue.getUid()) || !micMetadataValue.getUid()
                .equals(uid)) {
            throw new MicNotBelongYouException();
        }
        if (micMetadataValue.getStatus() == MicStatus.CLOSE.getStatus()) {
            this.updateVoiceRoomMicInfo(chatroomId, uid, micIndex,
                    MicOperateStatus.OPEN_MIC.getStatus(), Boolean.FALSE, roomId);
        } else {
            throw new MicStatusCannotBeModifiedException();
        }

    }

    @Override
    public void leaveMic(String uid, String chatroomId, Integer micIndex, String roomId) {

        if (micIndex == null || micIndex < 0) {
            return;
        }

        MicMetadataValue micMetadataValue = buildMicMetadataValue(chatroomId, micIndex);

        if (StringUtils.isEmpty(micMetadataValue.getUid()) || !micMetadataValue.getUid()
                .equals(uid)) {
            throw new MicNotBelongYouException();
        }

        this.updateVoiceRoomMicInfo(chatroomId, uid, micIndex,
                MicOperateStatus.LEAVE_MIC.getStatus(), Boolean.FALSE, roomId);

    }

    @Override
    public void muteMic(String chatroomId, Integer micIndex, String roomId) {

        MicMetadataValue micMetadataValue = buildMicMetadataValue(chatroomId, micIndex);

        if (micMetadataValue.getStatus() == MicStatus.MUTE.getStatus()
                || micMetadataValue.getStatus() == MicStatus.LOCK_AND_MUTE.getStatus()) {
            throw new MicStatusCannotBeModifiedException();
        }

        this.updateVoiceRoomMicInfo(chatroomId, null, micIndex,
                MicOperateStatus.MUTE_MIC.getStatus(), Boolean.TRUE, roomId);

    }

    @Override
    public void unMuteMic(String chatroomId, Integer micIndex, String roomId) {

        MicMetadataValue micMetadataValue = buildMicMetadataValue(chatroomId, micIndex);

        if (micMetadataValue.getStatus() != MicStatus.MUTE.getStatus()
                && micMetadataValue.getStatus() != MicStatus.LOCK_AND_MUTE.getStatus()) {
            throw new MicStatusCannotBeModifiedException();
        }

        this.updateVoiceRoomMicInfo(chatroomId, null, micIndex,
                MicOperateStatus.UNMUTE_MIC.getStatus(), Boolean.TRUE, roomId);

    }

    @Override
    public void kickUserMic(String chatroomId, Integer micIndex, String uid, String roomId) {

        MicMetadataValue micMetadataValue = buildMicMetadataValue(chatroomId, micIndex);

        if (StringUtils.isEmpty(micMetadataValue.getUid())) {
            throw new MicStatusCannotBeModifiedException();
        }

        if (!micMetadataValue.getUid().equals(uid)) {
            throw new MicNotCurrentUserException();
        }

        this.updateVoiceRoomMicInfo(chatroomId, null, micIndex,
                MicOperateStatus.KICK_MIC.getStatus(), Boolean.TRUE, roomId);

    }

    @Override
    public void lockMic(String chatroomId, Integer micIndex, String roomId) {

        MicMetadataValue micMetadataValue = buildMicMetadataValue(chatroomId, micIndex);

        if (micMetadataValue.getStatus() == MicStatus.LOCK.getStatus()
                || micMetadataValue.getStatus() == MicStatus.LOCK_AND_MUTE.getStatus()) {
            throw new MicStatusCannotBeModifiedException();
        }

        this.updateVoiceRoomMicInfo(chatroomId, null, micIndex,
                MicOperateStatus.LOCK_MIC.getStatus(), Boolean.TRUE, roomId);

    }

    @Override
    public void unLockMic(String chatroomId, Integer micIndex, String roomId) {

        MicMetadataValue micMetadataValue = buildMicMetadataValue(chatroomId, micIndex);

        if (micMetadataValue.getStatus() != MicStatus.LOCK.getStatus()
                && micMetadataValue.getStatus() != MicStatus.LOCK_AND_MUTE.getStatus()) {
            throw new MicStatusCannotBeModifiedException();
        }

        this.updateVoiceRoomMicInfo(chatroomId, null, micIndex,
                MicOperateStatus.UNLOCK_MIC.getStatus(), Boolean.TRUE, roomId);

    }

    @Override
    public void invite(VoiceRoom roomInfo, Integer index, String uid) {
        UserDTO userDTO = this.userService.getByUid(uid);
        if (userDTO == null) {
            throw new BaseException(ErrorCodeEnum.user_not_exist);
        }
        UserDTO ownerUser = userService.getByUid(roomInfo.getOwner());

        Map<String, Object> customExtensions = new HashMap<>();
        String jsonUser = "";
        try {
            jsonUser = objectMapper.writeValueAsString(userDTO);
        } catch (Exception e) {
            log.error("write user json failed | uid={}, user={}, e=", uid,
                    userDTO, e);
        }
        customExtensions.put("user", jsonUser);
        if (index != null) {
            customExtensions.put("mic_index", index.toString());
        }
        customExtensions.put("room_id", roomInfo.getRoomId());
        this.imApi.sendChatRoomCustomMessage(ownerUser.getChatUid(), roomInfo.getChatroomId(),
                CustomEventType.INVITE_SITE.getValue(), customExtensions, new HashMap<>());
    }

    @Override
    public Boolean agreeInvite(VoiceRoom roomInfo, String uid, Integer micIndex) {
        if (micIndex == null) {
            return setRoomMicInfo(roomInfo, uid, null, Boolean.TRUE);
        } else {
            return setRoomMicInfo(roomInfo, uid, micIndex, Boolean.FALSE);
        }

    }

    @Override
    public Boolean refuseInvite(VoiceRoom roomInfo, String uid) {

        UserDTO userDTO = this.userService.getByUid(uid);

        if (userDTO == null) {
            throw new BaseException(ErrorCodeEnum.user_not_exist);
        }

        Map<String, Object> customExtensions = new HashMap<>();
        try {
            customExtensions.put("user", objectMapper.writeValueAsString(userDTO));
        } catch (JsonProcessingException e) {
            log.error("write user json failed | uid={}, user={}, e=", uid,
                    userDTO, e);
        }
        customExtensions.put("room_id", roomInfo.getRoomId());
        this.imApi.sendChatRoomCustomMessage(userDTO.getChatUid(), roomInfo.getChatroomId(),
                CustomEventType.INVITE_REFUSED.getValue(), customExtensions, new HashMap<>());

        return Boolean.TRUE;

    }

    @Override
    public void exchangeMic(String chatroomId, Integer from, Integer to, String uid,
            String roomId) {

        if (from == 0) {
            throw new MicStatusCannotBeModifiedException();
        }

        String fromMicKey = buildMicKey(from);
        String toMicKey = buildMicKey(to);

        Map<String, String> metadata =
                imApi.listChatRoomMetadata(chatroomId, Arrays.asList(fromMicKey, toMicKey))
                        .getMetadata();

        if (metadata.containsKey(fromMicKey) && metadata.containsKey(toMicKey)) {

            MicMetadataValue fromMicMetadataValue = null;

            MicMetadataValue toMicMetadataValue = null;

            try {
                fromMicMetadataValue = objectMapper
                        .readValue(metadata.get(fromMicKey), MicMetadataValue.class);

                toMicMetadataValue =
                        objectMapper.readValue(metadata.get(toMicKey), MicMetadataValue.class);

            } catch (JsonProcessingException e) {
                log.error(
                        "parse voice room micMetadataValue json failed | chatroomId={}, uid={},"
                                + " json={}, e=", chatroomId, uid, e);
            }

            if (fromMicMetadataValue == null || toMicMetadataValue == null) {
                throw new MicInitException();
            }

            if (StringUtils.isEmpty(fromMicMetadataValue.getUid()) || !fromMicMetadataValue.getUid()
                    .equals(uid)) {
                throw new MicNotBelongYouException();
            }

            if (toMicMetadataValue.getStatus() != MicStatus.FREE.getStatus()) {
                throw new MicStatusCannotBeModifiedException();
            }

            this.exchangeMicInfo(chatroomId, uid, from, to, roomId);

        } else {
            throw new MicInitException();
        }

    }

    private void exchangeMicInfo(String chatroomId, String uid, Integer from, Integer to,
            String roomId) {

        String fromMicKey = buildMicKey(from);

        String toMicKey = buildMicKey(to);

        RLock micFromLock = redisson.getLock(buildMicLockKey(from, chatroomId));
        RLock micToLock = redisson.getLock(buildMicLockKey(to, chatroomId));
        boolean fromLockKey = false;
        boolean toLockKey = false;
        try {
            fromLockKey = micFromLock.tryLock(5000, TimeUnit.MILLISECONDS);
            toLockKey = micToLock.tryLock(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("get redis lock error", e);
        }
        if (!fromLockKey || !toLockKey) {
            throw new BaseException(ErrorCodeEnum.mic_is_concurrent_operation);
        }

        try {

            Map<String, String> metadata =
                    imApi.listChatRoomMetadata(chatroomId, Arrays.asList(fromMicKey, toMicKey))
                            .getMetadata();

            if (metadata.containsKey(fromMicKey) && metadata.containsKey(toMicKey)) {

                MicMetadataValue fromMicMetadataValue = null;

                MicMetadataValue toMicMetadataValue = null;

                try {
                    fromMicMetadataValue = objectMapper
                            .readValue(metadata.get(fromMicKey), MicMetadataValue.class);

                    toMicMetadataValue =
                            objectMapper.readValue(metadata.get(toMicKey), MicMetadataValue.class);

                } catch (JsonProcessingException e) {
                    log.error(
                            "parse voice room micMetadataValue json failed | chatroomId={}, uid={},"
                                    + " json={}, e=", chatroomId, uid, e);
                }

                if (fromMicMetadataValue == null || toMicMetadataValue == null) {
                    throw new MicInitException();
                }

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

                this.voiceRoomUserService
                        .updateVoiceRoomUserMicIndex(roomId, fromMicMetadataValue.getUid(), to);

            } else {
                throw new MicInitException();
            }

        } finally {
            if (fromLockKey) {
                micFromLock.unlock();
            }
            if (toLockKey) {
                micToLock.unlock();
            }
        }

    }

    private void updateVoiceRoomMicInfo(String chatroomId, String uid, Integer micIndex,
            Integer micOperateStatus, Boolean isAdminOperate, String roomId) {
        String metadataKey = buildMicKey(micIndex);
        String redisLockKey = buildMicLockKey(micIndex, chatroomId);

        RLock micLock = redisson.getLock(redisLockKey);
        boolean locked = micLock.isLocked();
        if (!locked) {
            throw new VoiceRoomException("400403", "the room mic can not be modified!",
                    HttpStatus.FORBIDDEN);
        }

        try {
            boolean lock = micLock.tryLock(100, TimeUnit.MILLISECONDS);
            if (!lock) {
                throw new VoiceRoomException("400403", "the room mic can not be modified!",
                        HttpStatus.FORBIDDEN);
            }
        } catch (InterruptedException e) {
            log.error("the thread has been interrupted!");
            throw new VoiceRoomException("400500", "update room mic info failed",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {

            Map<String, String> metadata =
                    imApi.listChatRoomMetadata(chatroomId, Arrays.asList(metadataKey))
                            .getMetadata();
            if (metadata.containsKey(metadataKey)) {

                MicMetadataValue micMetadataValue =
                        JSONObject
                                .parseObject(metadata.get(metadataKey), MicMetadataValue.class);

                Integer updateStatus = null;
                String updateUid = micMetadataValue.getUid();

                String roomUserUid = null;
                Integer roomUsermicIndex = -1;

                if (!Boolean.TRUE.equals(isAdminOperate) && !StringUtils
                        .isEmpty(micMetadataValue.getUid())
                        && !micMetadataValue.getUid()
                        .equals(uid)) {
                    throw new MicNotBelongYouException();
                }

                switch (MicOperateStatus.parse(micOperateStatus)) {
                    case UP_MIC:
                        if (micMetadataValue.getStatus() == MicStatus.FREE.getStatus() || (
                                micMetadataValue.getStatus() == MicStatus.MUTE.getStatus()
                                        && StringUtils.isEmpty(micMetadataValue.getUid()))) {
                            if (micMetadataValue.getStatus() == MicStatus.FREE.getStatus()) {
                                updateStatus = MicStatus.NORMAL.getStatus();
                            } else {
                                updateStatus = MicStatus.MUTE.getStatus();
                            }
                            updateUid = uid;
                            roomUserUid = uid;
                            roomUsermicIndex = micIndex;
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
                        if (micIndex == 0) {
                            throw new MicStatusCannotBeModifiedException();
                        }
                        updateStatus = MicStatus.FREE.getStatus();
                        updateUid = null;
                        roomUserUid = uid;
                        break;
                    case MUTE_MIC:
                        if (micIndex == 0) {
                            throw new MicStatusCannotBeModifiedException();
                        }
                        if (Boolean.TRUE.equals(isAdminOperate)
                                && micMetadataValue.getStatus() != MicStatus.MUTE.getStatus()
                                && micMetadataValue.getStatus() != MicStatus.LOCK_AND_MUTE
                                .getStatus()) {
                            updateStatus = MicStatus.MUTE.getStatus();
                            if (micMetadataValue.getStatus() == MicStatus.LOCK.getStatus()) {
                                updateStatus = MicStatus.LOCK_AND_MUTE.getStatus();
                            }
                        } else {
                            throw new MicStatusCannotBeModifiedException();
                        }
                        break;
                    case UNMUTE_MIC:
                        if (Boolean.TRUE.equals(isAdminOperate)
                                && (micMetadataValue.getStatus() == MicStatus.MUTE
                                .getStatus()
                                || micMetadataValue.getStatus() == MicStatus.LOCK_AND_MUTE
                                .getStatus())) {
                            updateStatus = MicStatus.NORMAL.getStatus();
                            if (StringUtils.isEmpty(micMetadataValue.getUid())) {
                                updateStatus = MicStatus.FREE.getStatus();
                            }
                            if (micMetadataValue.getStatus() == MicStatus.LOCK_AND_MUTE
                                    .getStatus()) {
                                updateStatus = MicStatus.LOCK.getStatus();
                            }
                        } else {
                            throw new MicStatusCannotBeModifiedException();
                        }
                        break;
                    case LOCK_MIC:
                        if (micIndex == 0) {
                            throw new MicStatusCannotBeModifiedException();
                        }
                        if (Boolean.TRUE.equals(isAdminOperate)
                                && micMetadataValue.getStatus() != MicStatus.LOCK
                                .getStatus()
                                && micMetadataValue.getStatus() != MicStatus.LOCK_AND_MUTE
                                .getStatus()) {
                            updateStatus = MicStatus.LOCK.getStatus();
                            if (micMetadataValue.getStatus() == MicStatus.MUTE.getStatus()) {
                                updateStatus = MicStatus.LOCK_AND_MUTE.getStatus();
                            }
                            updateUid = null;
                            roomUserUid = micMetadataValue.getUid();
                        } else {
                            throw new MicStatusCannotBeModifiedException();
                        }
                        break;
                    case UNLOCK_MIC:
                        if (Boolean.TRUE.equals(isAdminOperate)
                                && (micMetadataValue.getStatus() == MicStatus.LOCK
                                .getStatus()
                                || micMetadataValue.getStatus() == MicStatus.LOCK_AND_MUTE
                                .getStatus())) {
                            updateStatus = MicStatus.FREE.getStatus();
                            if (micMetadataValue.getStatus() == MicStatus.LOCK_AND_MUTE
                                    .getStatus()) {
                                updateStatus = MicStatus.MUTE.getStatus();
                            }
                            updateUid = null;
                        } else {
                            throw new MicStatusCannotBeModifiedException();
                        }
                        break;
                    case KICK_MIC:
                        if (micIndex == 0) {
                            throw new MicStatusCannotBeModifiedException();
                        }
                        if (Boolean.TRUE.equals(isAdminOperate) && !StringUtils
                                .isEmpty(micMetadataValue.getUid())) {
                            updateStatus = MicStatus.FREE.getStatus();
                            updateUid = null;
                            roomUserUid = micMetadataValue.getUid();
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

                if (!StringUtils.isEmpty(roomUserUid)) {
                    this.voiceRoomUserService
                            .updateVoiceRoomUserMicIndex(roomId, roomUserUid, roomUsermicIndex);
                }

            } else {
                throw new MicInitException();
            }

        } finally {
            if (micLock.isLocked() && micLock.isHeldByCurrentThread()) {
                micLock.unlock();
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
            int index = -1;
            try {
                index = Integer.parseInt(key.split("_")[1]);
            } catch (Exception e) {
                log.error(
                        "mic index less than zero,index:{}", index, e);
            }
            if (index < 0) {
                throw new MicInitException();
            }
            MicInfo micInfo =
                    MicInfo.builder().status(micMetadataValue.getStatus()).micIndex(index)
                            .member(user)
                            .build();
            micInfos.add(micInfo);

        }
        micInfos = micInfos.stream().sorted(Comparator.comparing(MicInfo::getMicIndex)).collect(
                Collectors.toList());
        return micInfos;
    }

    private String buildMicKey(Integer micIndex) {
        return METADATA_PREFIX_KEY + "_" + micIndex;
    }

    private String buildMicLockKey(Integer micIndex, String chatRoomId) {
        return chatRoomId + "_" + buildMicKey(micIndex) + "_lock";
    }

    private MicMetadataValue buildMicMetadataValue(String chatroomId, Integer micIndex) {

        String metadataKey = buildMicKey(micIndex);

        //todo 这个地方如果imApi失败了，会报空指针，最好处理一下
        Map<String, String> metadata =
                imApi.listChatRoomMetadata(chatroomId, Arrays.asList(metadataKey)).getMetadata();

        if (metadata.containsKey(metadataKey)) {

            MicMetadataValue micMetadataValue = null;

            try {
                micMetadataValue = objectMapper
                        .readValue(metadata.get(metadataKey), MicMetadataValue.class);

            } catch (JsonProcessingException e) {
                log.error(
                        "parse voice room micMetadataValue json failed | chatroomId={}, micIndex={}",
                        chatroomId, micIndex, e);
            }

            if (micMetadataValue == null) {
                throw new MicInitException();
            }
            return micMetadataValue;
        } else {
            throw new MicInitException();
        }
    }
}
