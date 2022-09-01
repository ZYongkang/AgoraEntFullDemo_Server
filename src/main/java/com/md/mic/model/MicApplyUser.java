package com.md.mic.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mic_apply_user")
public class MicApplyUser {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private String roomId;

    private String uid;

    private Integer micIndex;
}
