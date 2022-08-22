package com.md.service.model.form;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreateForm {

    @ApiModelProperty("房间名字")
    private String name;

    @ApiModelProperty("是否公开 0 公开 1加密")
    private Integer isPrivate = 0;

    @ApiModelProperty("密码")
    private String password;

    @ApiModelProperty("创建用户")
    private String userNo;

    @ApiModelProperty("房间唯一标识")
    private String roomNo;

    @ApiModelProperty("图标")
    private String icon;

    @ApiModelProperty("背景")
    private String bgOption;

    @ApiModelProperty("音效")
    private String soundEffect;

    @ApiModelProperty("美声")
    private String belCanto;

    @ApiModelProperty("是否合唱 0 不合唱 1合唱")
    private Integer isChorus;

}
