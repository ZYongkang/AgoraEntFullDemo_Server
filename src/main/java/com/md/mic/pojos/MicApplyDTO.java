package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MicApplyDTO {

    private Integer index;

    private UserDTO user;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonCreator
    public MicApplyDTO(Integer index, UserDTO user,
            @JsonProperty("created_at") Long createdAt) {
        this.index = index;
        this.user = user;
        this.createdAt = createdAt;
    }
}
