package com.md.mic.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@TableName("easemob_user")
public class EasemobUser {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String uid;

    private String chatId;

    private String chatUuid;

    @JsonCreator
    public EasemobUser(Integer id, String uid, String chatId, String chatUuid) {
        this.id = id;
        this.uid = uid;
        this.chatId = chatId;
        this.chatUuid = chatUuid;
    }
}
