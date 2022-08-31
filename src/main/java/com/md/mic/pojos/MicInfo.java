package com.md.mic.pojos;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MicInfo {

    private Integer index;

    private Integer status;

    private UserDTO user;

}
