package com.md.mic.model;

import com.baomidou.mybatisplus.annotation.*;
import com.md.service.utils.MdStringUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.codec.digest.Md5Crypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.apache.commons.codec.Charsets.UTF_8;

@Value
@TableName("user")
@Builder(toBuilder = true)
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private String uid;

    private String name;

    private String portrait;

    private String deviceId;

    private String phone;

    public static User create(String name, String deviceId, String portrait) {
        return create(name, deviceId, portrait, null);
    }

    public static User create(String name, String deviceId, String portrait, String phone) {
        String uid = buildUid(name, deviceId);
        return User.builder().uid(uid)
                .name(name)
                .deviceId(deviceId)
                .portrait(portrait)
                .phone(phone)
                .build();
    }
    private static String buildUid(String name, String deviceId) {


            return "jrHaFalmZJPlP9nN9YFKOgL=";

    }
}
