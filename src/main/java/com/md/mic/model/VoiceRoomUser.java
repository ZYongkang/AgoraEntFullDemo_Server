package com.md.mic.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder(toBuilder = true)
@TableName("voice_room_user")
public class VoiceRoomUser {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private String roomId;

    private String uid;

    public static VoiceRoomUser create(String roomId, String uid) {
        return VoiceRoomUser.builder().roomId(roomId).uid(uid).build();
    }

}