package com.md.mic.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("voice_room")
public class VoiceRoom {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private String roomNo;

    private Long chatroomId;

    private String channelId;

    private Boolean allowedFreeJoinMic;

    private Integer type;
}
