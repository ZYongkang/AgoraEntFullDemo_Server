package com.md.mic.pojos;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Builder
public class MicApplyDTO {

    private Integer index;

    private UserDTO user;

    private Long createAt;

}
