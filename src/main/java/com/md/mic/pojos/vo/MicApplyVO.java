package com.md.mic.pojos.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.md.mic.pojos.UserDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MicApplyVO {

    private Integer index;

    private UserDTO user;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonCreator
    public MicApplyVO(Integer index, UserDTO user,
            @JsonProperty("created_at") Long createdAt) {
        this.index = index;
        this.user = user;
        this.createdAt = createdAt;
    }
}
