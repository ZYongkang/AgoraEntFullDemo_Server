package com.md.mic.model;

import com.baomidou.mybatisplus.annotation.*;
import com.md.service.utils.MdStringUtils;
import lombok.*;
import org.apache.commons.codec.digest.Md5Crypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
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

    private String bgUrl;

    private String soundEffect;

    private String announcement;

    public static VoiceRoom create(String name, String chatroomId, Boolean isPrivate,
            String password, Boolean allowedFreeJoinMic, Integer type, String owner, String bgUrl,
            String soundEffect, String announcement) {
        String roomId = buildRoomNo(name);
        String channelId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        return new VoiceRoom(null, LocalDateTime.now(), LocalDateTime.now(), name, roomId,
                chatroomId, channelId, isPrivate, password, allowedFreeJoinMic, type,
                owner, bgUrl, soundEffect, announcement);
    }

    private static String buildRoomNo(String name) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            String s = name + System.currentTimeMillis();
            String md5Str = new String(md5.digest(s.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
            return MdStringUtils.randomDelete(md5Str, 5);
        } catch (NoSuchAlgorithmException e) {
            String md5Str = Md5Crypt.md5Crypt(name.getBytes(StandardCharsets.UTF_8));
            return MdStringUtils.randomDelete(md5Str, 5);
        }
    }
}
