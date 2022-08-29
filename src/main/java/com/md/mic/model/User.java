package com.md.mic.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

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

}
