package com.md.common.im;

import com.easemob.im.server.EMException;
import com.easemob.im.server.EMService;
import com.easemob.im.server.api.metadata.chatroom.AutoDelete;
import com.easemob.im.server.api.metadata.chatroom.delete.ChatRoomMetadataDeleteResponse;
import com.easemob.im.server.api.metadata.chatroom.get.ChatRoomMetadataGetResponse;
import com.easemob.im.server.api.metadata.chatroom.set.ChatRoomMetadataSetResponse;
import com.easemob.im.server.model.EMKeyValue;
import com.easemob.im.server.model.EMPage;
import com.easemob.im.server.model.EMRoom;
import com.easemob.im.server.model.EMUser;
import com.easemob.im.shaded.io.netty.handler.timeout.TimeoutException;
import com.md.mic.common.constants.CustomMetricsName;
import com.md.mic.model.UserThirdAccount;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * imAPI。
 */
@Slf4j
@Service
public class ImApi {

    @Autowired
    private EMService emService;

    @Value("${http.request.timeout:PT10s}")
    private Duration timeout;

    @Autowired
    private PrometheusMeterRegistry registry;

    private static final String METRICS_REGISTRY_NAME = "easemob.im.api.http.request";

    /**
     * 不指定密码创建用户
     *
     * @param uid
     * @param username
     * @return
     * @throws EMException
     */
    public UserThirdAccount createUser(@Nonnull String uid, @Nonnull String username)
            throws EMException {
        return createUser(uid, username, null);
    }

    /**
     * 创建用户
     *
     * @param uid
     * @param username
     * @param password
     * @return
     */
    public UserThirdAccount createUser(@Nonnull String uid, @Nonnull String username,
            String password) {
        if (StringUtils.isBlank(password)) {
            password = UUID.randomUUID().toString().replace("-", "");
        }
        try {
            EMUser emUser = this.emService.user().create(username, password).block();
            return UserThirdAccount.builder().uid(uid).chatId(emUser.getUsername())
                    .chatUuid(emUser.getUuid()).build();
        } catch (TimeoutException e) {
            log.error("createUser request timeout | uid={}, username={}",
                    uid, username, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");

        } catch (EMException e) {
            log.error("createUser request easemob failed,userName:{},uid:{}", username, uid, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason",
                    "InternalServerError", "result", "error");
        } catch (Exception e) {
            log.error("createUser failed | uid={}, username={}",
                    uid, username, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }
        return null;

    }

    /**
     * 删除用户
     *
     * @param userName: 用户名
     * @throws EMException
     */
    public void deleteUser(@Nonnull String userName)
            throws EMException {

        try {
            this.emService.user().delete(userName).block();
        } catch (TimeoutException e) {
            log.error("deleteUser request timeout | username={}",
                    userName, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error("deleteUser request easemob failed,userName:{}", userName, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason",
                    "InternalServerError", "result", "error");
        } catch (Exception e) {
            log.error("deleteUser failed | username={}",
                    userName, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }

    }

    /**
     * 创建聊天室
     *
     * @param chatRoomName:    聊天室的名称
     * @param owner:           房主
     * @param members:         成员列表
     * @param description:房间描述
     * @return String :聊天室id
     * @throws EMException
     */
    public String createChatRoom(@Nonnull String chatRoomName, @Nonnull String owner,
            @Nonnull List<String> members,
            @Nonnull String description)
            throws EMException {

        try {
            return emService.room().createRoom(chatRoomName, description, owner, members, 200)
                    .block();
        } catch (TimeoutException e) {
            log.error(
                    "createRoom request timeout,chatRoomName:{},owner:{},members:{},description:{}",
                    chatRoomName, owner, members, description, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error(
                    "createRoom request easemob failed,chatRoomName:{},owner:{},members:{},description:{}",
                    chatRoomName, owner, members, description, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason",
                    "InternalServerError", "result", "error");
        } catch (Exception e) {
            log.error(
                    "createRoom failed,chatRoomName:{},owner:{},members:{},description:{}",
                    chatRoomName, owner, members, description, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }
        return null;

    }

    /**
     * 销毁聊天室
     *
     * @param chatRoomId: 聊天室id
     * @throws EMException
     */
    public void deleteChatRoom(@Nonnull String chatRoomId)
            throws EMException {

        try {
            emService.room().destroyRoom(chatRoomId).block();
        } catch (TimeoutException e) {
            log.error(
                    "deleteChatRoom request timeout,chatRoomId:{}", chatRoomId, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error(
                    "deleteChatRoom request easemob failed,chatRoomId:{}", chatRoomId, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason",
                    "InternalServerError", "result", "error");
        } catch (Exception e) {
            log.error(
                    "deleteChatRoom failed,chatRoomId:{}", chatRoomId, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }

    }

    /**
     * 获取聊天室详情
     *
     * @param chatRoomId: 聊天室id
     * @return EMRoom :聊天室详情
     * @throws EMException
     */
    public EMRoom getChatRoomInfo(@Nonnull String chatRoomId) throws EMException {

        try {
            return emService.room().getRoom(chatRoomId)
                    .block();
        } catch (TimeoutException e) {
            log.error(
                    "getChatRoomInfo request timeout,chatRoomId:{}", chatRoomId, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error(
                    "getChatRoomInfo request easemob failed,chatRoomId:{}", chatRoomId, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason",
                    "InternalServerError", "result", "error");
        } catch (Exception e) {
            log.error(
                    "getChatRoomInfo failed,chatRoomId:{}", chatRoomId, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }
        return null;
    }

    /**
     * 分页获取聊天室列表
     *
     * @param limit  返回多少个聊天室id
     * @param cursor 开始位置
     * @return 聊天室id列表和cursor
     * @throws EMException
     */
    public EMPage<String> listChatRooms(int limit, String cursor)
            throws EMException {

        try {
            return emService.room().listRooms(limit, cursor).block();
        } catch (TimeoutException e) {
            log.error(
                    "listChatRooms request timeout,listChatRooms error,limit:{},cursor:{},members:{},description:{}",
                    limit, cursor, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error(
                    "listChatRooms request easemob failed,listChatRooms error,limit:{},cursor:{},members:{},description:{}",
                    limit, cursor, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason",
                    "InternalServerError", "result", "error");
        } catch (Exception e) {
            log.error(
                    "listChatRooms failed,limit:{},cursor:{},members:{},description:{}",
                    limit, cursor, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }
        return null;

    }

    /**
     * 分页获取聊天室成员列表
     *
     * @param chatRoomId 聊天室id
     * @param limit      返回多少个聊天室成员
     * @param cursor     开始位置
     * @param sort       聊天室成员排序方法 asc:根据加入顺序升序排序  desc:根据加入顺序降序排序
     * @return 聊天室用户的userName列表和cursor
     * @throws EMException
     */
    public EMPage<String> listChatRoomMembers(@Nonnull String chatRoomId, int limit, String cursor,
            String sort)
            throws EMException {

        try {
            return emService.room().listRoomMembers(chatRoomId, limit, cursor, sort).block();
        } catch (TimeoutException e) {
            log.error(
                    "listChatRoomMembers request timeout,listChatRoomMembers error,chatRoomId:{},limit:{},cursor:{}",
                    chatRoomId, limit, cursor, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error("listChatRoomMembers request easemob failed,chatRoomId:{},limit:{},cursor:{}",
                    chatRoomId, limit, cursor, e);
            throw e;
        } catch (Exception e) {
            log.error(
                    "listChatRoomMembers failed,listChatRoomMembers error,chatRoomId:{},limit:{},cursor:{}",
                    chatRoomId, limit, cursor, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }
        return null;

    }

    /**
     * 从聊天室移除成员。
     *
     * @param chatRoomId 聊天室id
     * @param userName   聊天室成员
     * @throws EMException
     */
    public void removeChatRoomMember(@Nonnull String chatRoomId, @Nonnull String userName)
            throws EMException {

        try {
            emService.room().removeRoomMember(chatRoomId, userName).block();
        } catch (TimeoutException e) {
            log.error(
                    "removeChatRoomMember request timeout,removeChatRoomMember error,chatRoomId:{},userName:{}",
                    chatRoomId, userName, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error("removeChatRoomMember request easemob failed,chatRoomId:{},userName:{}",
                    chatRoomId, userName, e);
            throw e;
        } catch (Exception e) {
            log.error(
                    "removeChatRoomMember failed,removeChatRoomMember error,chatRoomId:{},userName:{}",
                    chatRoomId, userName, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }
    }

    /**
     * 设置群公告
     *
     * @param chatRoomId   聊天室id
     * @param announcement 通知
     * @throws EMException
     */
    public void setAnnouncement(@Nonnull String chatRoomId, @Nonnull String announcement)
            throws EMException {

        try {
            emService.room().updateRoomAnnouncement(chatRoomId, announcement).block();
        } catch (TimeoutException e) {
            log.error(
                    "setAnnouncement request timeout,chatRoomId:{},announcement:{}",
                    chatRoomId, announcement, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error("setAnnouncement request easemob failed,chatRoomId:{},announcement:{}",
                    chatRoomId, announcement, e);
            throw e;
        } catch (Exception e) {
            log.error(
                    "setAnnouncement failed,chatRoomId:{},announcement:{}",
                    chatRoomId, announcement, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }
    }

    /**
     * 获取群公告
     *
     * @param chatRoomId 聊天室id
     * @throws EMException
     */
    public String getAnnouncement(@Nonnull String chatRoomId)
            throws EMException {

        try {
            return emService.room().getRoomAnnouncement((chatRoomId)).block();
        } catch (EMException e) {
            log.error("server error,getAnnouncement error,chatRoomId:{}",
                    chatRoomId, e);
            throw e;
        }
    }

    /**
     * 发送自定义消息
     *
     * @param fromUserName     发送的成员
     * @param toChatRoomId     接收的聊天室id
     * @param customEvent      自定义消息类型
     * @param customExtensions 自定义消息内容
     * @param extension        自定义消息扩展
     * @throws EMException
     */
    public void sendChatRoomCustomMessage(@Nonnull String fromUserName,
            @Nonnull String toChatRoomId,
            @Nonnull String customEvent, @Nonnull Map<String, Object> customExtensions,
            Map<String, Object> extension)
            throws EMException {

        try {
            emService.message().send()
                    .fromUser(fromUserName)
                    .toRoom(toChatRoomId)
                    .custom(msg -> msg.customEvent(customEvent)
                            .customExtensions(EMKeyValue.of(customExtensions)))
                    .extension(msg -> msg.addAll(EMKeyValue.of(extension)))
                    .send()
                    .block();
        } catch (TimeoutException e) {
            log.error(
                    "sendChatRoomCustomMessage request timeout,fromUserName:{},toChatRoomId:{},customEvent:{},customExtensions:{}",
                    fromUserName, toChatRoomId, customEvent, customExtensions, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error(
                    "sendChatRoomCustomMessage request easemob failed,fromUserName:{},toChatRoomId:{},customEvent:{},customExtensions:{}",
                    fromUserName, toChatRoomId, customEvent, customExtensions, e);
            throw e;
        } catch (Exception e) {
            log.error(
                    "sendChatRoomCustomMessage failed,,fromUserName:{},toChatRoomId:{},customEvent:{},customExtensions:{}",
                    fromUserName, toChatRoomId, customEvent, customExtensions, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }

    }

    /**
     * 发送自定义消息 个人
     *
     * @param fromUserName     发送的成员
     * @param toUserName       接收的成员
     * @param customEvent      自定义消息类型
     * @param customExtensions 自定义消息内容
     * @param extension        自定义消息扩展
     * @throws EMException
     */
    public void sendUserCustomMessage(@Nonnull String fromUserName,
            @Nonnull String toUserName,
            @Nonnull String customEvent, @Nonnull Map<String, Object> customExtensions,
            Map<String, Object> extension)
            throws EMException {

        try {
            emService.message().send()
                    .fromUser(fromUserName)
                    .toUser(toUserName)
                    .custom(msg -> msg.customEvent(customEvent)
                            .customExtensions(EMKeyValue.of(customExtensions)))
                    .extension(msg -> msg.addAll(EMKeyValue.of(extension)))
                    .send()
                    .block();
        } catch (TimeoutException e) {
            log.error(
                    "sendUserCustomMessage request timeout,fromUserName:{},toUserName:{},customEvent:{},customExtensions:{}",
                    fromUserName, toUserName, customEvent, customExtensions, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error(
                    "sendUserCustomMessage request easemob failed,fromUserName:{},toUserName:{},customEvent:{},customExtensions:{}",
                    fromUserName, toUserName, customEvent, customExtensions, e);
            throw e;
        } catch (Exception e) {
            log.error(
                    "sendUserCustomMessage failed,chatRoomId:{},fromUserName:{},toUserName:{},customEvent:{},customExtensions:{}",
                    fromUserName, toUserName, customEvent, customExtensions, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }

    }

    /**
     * 设置聊天室属性
     *
     * @param operator   操作人
     * @param chatRoomId 接收的聊天室id
     * @param metadata   属性k-v
     * @throws EMException
     */
    public ChatRoomMetadataSetResponse setChatRoomMetadata(@Nonnull String operator,
            @Nonnull String chatRoomId,
            @Nonnull Map<String, String> metadata,
            AutoDelete autoDelete)
            throws EMException {

        try {
            return emService.metadata()
                    .setChatRoomMetadata(operator, chatRoomId, metadata, autoDelete)
                    .block();
        } catch (TimeoutException e) {
            log.error(
                    "setChatRoomMetadata request timeout,fromUserName:{},operator:{},chatRoomId:{},metadata:{},autoDelete:{}",
                    operator, chatRoomId, metadata, autoDelete, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error(
                    "setChatRoomMetadata request easemob failed,operator:{},chatRoomId:{},metadata:{},autoDelete:{}",
                    operator, chatRoomId, metadata, autoDelete, e);
            throw e;
        } catch (Exception e) {
            log.error(
                    "setChatRoomMetadata failed,chatRoomId:{},operator:{},chatRoomId:{},metadata:{},autoDelete:{}",
                    operator, chatRoomId, metadata, autoDelete, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }
        return null;

    }

    /**
     * 删除聊天室属性
     *
     * @param operator   操作人
     * @param chatRoomId 接收的聊天室id
     * @param keys       属性k列表
     * @throws EMException
     */
    public ChatRoomMetadataDeleteResponse deleteChatRoomMetadata(@Nonnull String operator,
            @Nonnull String chatRoomId,
            @Nonnull List<String> keys)
            throws EMException {

        try {
            return emService.metadata().deleteChatRoomMetadata(operator, chatRoomId, keys)
                    .block();
        } catch (TimeoutException e) {
            log.error(
                    "deleteChatRoomMetadata request timeout,fromUserName:{},operator:{},chatRoomId:{},keys:{}",
                    operator, chatRoomId, keys, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error(
                    "deleteChatRoomMetadata request easemob failed,operator:{},chatRoomId:{},keys:{}",
                    operator, chatRoomId, keys, e);
            throw e;
        } catch (Exception e) {
            log.error(
                    "deleteChatRoomMetadata failed,chatRoomId:{},operator:{},chatRoomId:{},keys:{}",
                    operator, chatRoomId, keys, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }
        return null;

    }

    /**
     * 获取聊天室属性
     *
     * @param chatRoomId 接收的聊天室id
     * @param keys       属性k列表
     * @throws EMException
     */
    public ChatRoomMetadataGetResponse listChatRoomMetadata(@Nonnull String chatRoomId,
            List<String> keys)
            throws EMException {

        try {
            return emService.metadata().listChatRoomMetadata(chatRoomId, keys)
                    .block();
        } catch (TimeoutException e) {
            log.error(
                    "listChatRoomMetadata request timeout,chatRoomId:{},keys:{}", chatRoomId,
                    keys, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error("listChatRoomMetadata request easemob failed,chatRoomId:{},keys:{}",
                    chatRoomId,
                    keys, e);
            throw e;
        } catch (Exception e) {
            log.error(
                    "listChatRoomMetadata failed,chatRoomId:{},keys:{}", chatRoomId,
                    keys, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }
        return null;

    }

    public void kickChatroomMember(String chatroomId, String username) {
        try {
            emService.room().removeRoomMember(chatroomId, username)
                    .timeout(timeout)
                    .block();
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "success",
                    "result", "success");
        } catch (TimeoutException e) {
            log.error("kickChatroomMember request timeout | chatroomId={}, username={}",
                    chatroomId, username, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "timeout",
                    "result", "error");
        } catch (EMException e) {
            log.error("kickChatroomMember request easemob failed | chatroomId={}, username={}",
                    chatroomId, username, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason",
                    "InternalServerError", "result", "error");
        } catch (Exception e) {
            log.error("kickChatroomMember failed | chatroomId={}, username={}",
                    chatroomId, username, e);
            registry.counter(CustomMetricsName.ImHttpRequestCounter, "reason", "unknown",
                    "result", "error");
        }

    }
}
