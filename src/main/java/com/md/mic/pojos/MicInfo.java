package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MicInfo {

    private Integer index;

    private Integer status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserDTO user;

}
