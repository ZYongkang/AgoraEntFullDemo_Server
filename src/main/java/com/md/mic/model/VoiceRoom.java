package com.md.mic.model;

import com.baomidou.mybatisplus.annotation.*;
import com.md.service.utils.MdStringUtils;
import lombok.*;
import org.apache.commons.codec.digest.Md5Crypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.apache.commons.codec.Charsets.UTF_8;

@Value
@EqualsAndHashCode
@TableName("voice_room")
@Builder(toBuilder = true)
public class VoiceRoom {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private String name;

    private String roomId;

    private String chatroomId;

    private String channelId;

    private Boolean isPrivate;

    private String password;

    private Boolean allowedFreeJoinMic;

    private Integer type;

    private String owner;

    private String soundEffect;

    private String announcement;

    public static VoiceRoom create(String name, String chatroomId, Boolean isPrivate,
            String password, Boolean allowedFreeJoinMic, Integer type, String owner,
            String soundEffect) {
        String roomId = buildRoomNo(name);
        String channelId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        return VoiceRoom.builder().name(name).roomId(roomId).chatroomId(chatroomId)
                .channelId(channelId).isPrivate(isPrivate).password(password)
                .allowedFreeJoinMic(allowedFreeJoinMic).type(type)
                .owner(owner).soundEffect(soundEffect)
                .build();
    }

    private static String buildRoomNo(String name) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            String s = name + System.currentTimeMillis();
            String encode = Base64.getEncoder().encodeToString(md5.digest(s.getBytes(UTF_8)));
            return MdStringUtils.randomDelete(encode, 5);
        } catch (NoSuchAlgorithmException e) {
            String s = name + System.currentTimeMillis();
            String md5Str = Md5Crypt.md5Crypt(s.getBytes(UTF_8));
            return MdStringUtils.randomDelete(md5Str, 5);
        }
    }

    public VoiceRoom updateName(String name) {
        return VoiceRoom.builder().id(id).name(name).roomId(roomId).chatroomId(chatroomId)
                .channelId(channelId).isPrivate(isPrivate).password(password)
                .allowedFreeJoinMic(allowedFreeJoinMic).type(type).owner(owner)
                .soundEffect(soundEffect).announcement(announcement)
                .build();
    }

    public VoiceRoom updateIsPrivate(Boolean isPrivate) {
        return VoiceRoom.builder().id(id).name(name).roomId(roomId).chatroomId(chatroomId)
                .channelId(channelId).isPrivate(isPrivate).password(password)
                .allowedFreeJoinMic(allowedFreeJoinMic).type(type).owner(owner)
                .soundEffect(soundEffect).announcement(announcement)
                .build();
    }

    public VoiceRoom updatePassword(String password) {
        return VoiceRoom.builder().id(id).name(name).roomId(roomId)
                .chatroomId(chatroomId).channelId(channelId).isPrivate(isPrivate)
                .password(password).allowedFreeJoinMic(allowedFreeJoinMic)
                .type(type).owner(owner).soundEffect(soundEffect).announcement(announcement)
                .build();
    }

    public VoiceRoom updateAllowedFreeJoinMic(Boolean allowedFreeJoinMic) {
        return VoiceRoom.builder().id(id).name(name).roomId(roomId)
                .chatroomId(chatroomId).channelId(channelId).isPrivate(isPrivate)
                .password(password).allowedFreeJoinMic(allowedFreeJoinMic)
                .type(type).owner(owner).soundEffect(soundEffect).announcement(announcement)
                .build();
    }

    public VoiceRoom updateType(Integer type) {
        return VoiceRoom.builder().id(id).name(name).roomId(roomId)
                .chatroomId(chatroomId).channelId(channelId).isPrivate(isPrivate)
                .password(password).allowedFreeJoinMic(allowedFreeJoinMic)
                .type(type).owner(owner).soundEffect(soundEffect).announcement(announcement)
                .build();
    }

    public VoiceRoom updateAnnouncement(String announcement) {
        return VoiceRoom.builder().id(id).name(name).roomId(roomId)
                .chatroomId(chatroomId).channelId(channelId).isPrivate(isPrivate)
                .password(password).allowedFreeJoinMic(allowedFreeJoinMic)
                .type(type).owner(owner).soundEffect(soundEffect).announcement(announcement)
                .build();
    }


}
