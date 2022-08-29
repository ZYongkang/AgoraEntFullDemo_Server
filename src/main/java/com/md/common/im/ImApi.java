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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * imAPI。
 */
@Slf4j
@Service
public class ImApi {

    @Autowired
    private EMService emService;

    /**
     * 创建用户
     *
     * @param userName: 用户名
     * @param password: 密码
     * @return 环信userName
     * @throws Exception
     */
    public String createUser(String userName, String password)
            throws Exception {

        if (userName == null) {
            throw new IllegalArgumentException("Paramer 'userName' is required");
        }

        if (password == null) {
            throw new IllegalArgumentException("Paramer 'password' is required");
        }

        try {
            return this.emService.user().create(userName, password).block().getUsername();
        } catch (EMException e) {
            log.error("server error", e);
            throw new IllegalArgumentException("server error");
        }

    }

    /**
     * 删除用户
     *
     * @param userName: 用户名
     * @throws Exception
     */
    public void deleteUser(String userName, String password)
            throws Exception {

        if (userName == null) {
            throw new IllegalArgumentException("Paramer 'userName' is required");
        }

        if (password == null) {
            throw new IllegalArgumentException("Paramer 'password' is required");
        }

        try {
            this.emService.user().delete(userName).block();
        } catch (EMException e) {
            log.error("server error", e);
            throw new IllegalArgumentException("server error");
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
     * @throws Exception
     */
    public String createChatRoom(String chatRoomName, String owner, List<String> members,
            String description)
            throws Exception {

        if (chatRoomName == null) {
            throw new IllegalArgumentException("Paramer 'chatroomName' is required");
        }

        if (owner == null) {
            throw new IllegalArgumentException("Paramer 'owner' is required");
        }

        if (CollectionUtils.isEmpty(members)) {
            throw new IllegalArgumentException("Paramer 'members' is required");
        }

        if (description == null) {
            throw new IllegalArgumentException("Paramer 'description' is required");
        }

        try {
            return emService.room().createRoom(chatRoomName, owner, description, members, 200)
                    .block();
        } catch (EMException e) {
            log.error("server error", e);
            throw new IllegalArgumentException("server error");
        }

    }

    /**
     * 获取聊天室详情
     *
     * @param chatRoomId: 聊天室id
     * @return EMRoom :聊天室详情
     * @throws Exception
     */
    public EMRoom getChatRoomInfo(String chatRoomId) throws Exception {

        if (chatRoomId == null) {
            throw new IllegalArgumentException("Paramer 'chatRoomId' is required");
        }

        try {
            return emService.room().getRoom(chatRoomId)
                    .block();
        } catch (EMException e) {
            log.error("server error", e);
            throw new IllegalArgumentException("server error");
        }

    }

    /**
     * 分页获取聊天室列表
     *
     * @param limit  返回多少个聊天室id
     * @param cursor 开始位置
     * @return 聊天室id列表和cursor
     * @throws Exception
     */
    public EMPage<String> listChatRooms(int limit, String cursor)
            throws Exception {

        try {
            return emService.room().listRooms(limit, cursor).block();
        } catch (EMException e) {
            log.error("server error", e);
            throw new IllegalArgumentException("server error");
        }

    }

    /**
     * 分页获取聊天室成员列表
     *
     * @param chatRoomId 聊天室id
     * @param limit      返回多少个聊天室成员
     * @param cursor     开始位置
     * @param sort       聊天室成员排序方法 asc:根据加入顺序升序排序  desc:根据加入顺序降序排序
     * @return 聊天室用户的userName列表和cursor
     * @throws Exception
     */
    public EMPage<String> listChatRoomMembers(String chatRoomId, int limit, String cursor,
            String sort)
            throws Exception {

        try {
            return emService.room().listRoomMembers(chatRoomId, limit, cursor, sort).block();
        } catch (EMException e) {
            log.error("server error", e);
            throw new IllegalArgumentException("server error");
        }

    }

    /**
     * 从聊天室移除成员。
     *
     * @param chatRoomId 聊天室id
     * @param userName   聊天室成员
     * @throws Exception
     */
    public void removeChatRoomMember(String chatRoomId, String userName)
            throws Exception {

        try {
            emService.room().removeRoomMember(chatRoomId, userName).block();
        } catch (EMException e) {
            log.error("server error", e);
            throw new IllegalArgumentException("server error");
        }

    }

    /**
     * 发送自定义消息
     *
     * @param fromUserName     发送的成员
     * @param toChatRoomId     接收的聊天室id
     * @param customEvent      自定义消息类型
     * @param customContent    自定义消息内容
     * @param customExtensions 自定义消息扩展
     * @throws Exception
     */
    public void sendChatRoomCustomMessage(String fromUserName, String toChatRoomId,
            String customEvent, Map<String, Object> customContent,
            Map<String, Object> customExtensions)
            throws Exception {

        try {
            emService.message().send()
                    .fromUser(fromUserName)
                    .toRoom(toChatRoomId)
                    .custom(msg -> msg.customEvent(customEvent)
                            .customExtensions(EMKeyValue.of(customContent)))
                    .extension(msg -> msg.addAll(EMKeyValue.of(customExtensions)))
                    .send()
                    .block();
        } catch (EMException e) {
            log.error("server error", e);
            throw new IllegalArgumentException("server error");
        }

    }

    /**
     * 设置聊天室属性
     *
     * @param operator   操作人
     * @param chatRoomId 接收的聊天室id
     * @param metatdata  属性k-v
     * @throws Exception
     */
    public ChatRoomMetadataSetResponse setChatRoomMetadata(String operator, String chatRoomId,
            Map<String, String> metatdata,
            AutoDelete autoDelete)
            throws Exception {

        try {
            return emService.metadata()
                    .setChatRoomMetadata(operator, chatRoomId, metatdata, autoDelete)
                    .block();
        } catch (EMException e) {
            log.error("server error", e);
            throw new IllegalArgumentException("server error");
        }

    }

    /**
     * 删除聊天室属性
     *
     * @param operator   操作人
     * @param chatRoomId 接收的聊天室id
     * @param keys       属性k列表
     * @throws Exception
     */
    public ChatRoomMetadataDeleteResponse deleteChatRoomMetadata(String operator, String chatRoomId,
            List<String> keys)
            throws Exception {

        try {
            return emService.metadata().deleteChatRoomMetadata(operator, chatRoomId, keys)
                    .block();
        } catch (EMException e) {
            log.error("server error", e);
            throw new IllegalArgumentException("server error");
        }

    }

    /**
     * 获取聊天室属性
     *
     * @param chatRoomId 接收的聊天室id
     * @param keys       属性k列表
     * @throws Exception
     */
    public ChatRoomMetadataGetResponse listChatRoomMetadata(String chatRoomId, List<String> keys)
            throws Exception {

        try {
            return emService.metadata().listChatRoomMetadata(chatRoomId, keys)
                    .block();
        } catch (EMException e) {
            log.error("server error", e);
            throw new IllegalArgumentException("server error");
        }

    }

}
