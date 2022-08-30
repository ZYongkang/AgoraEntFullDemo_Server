package com.md.mic.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

@Data
@TableName("easemob_user")
@Builder
public class EasemobUser {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String uid;

    private String chatId;

    private String chatUuid;
}
