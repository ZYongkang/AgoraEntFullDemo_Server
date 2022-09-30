package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MicMetadataValue {

    private UserDTO member;

    private Integer status;

    public String getUid() {
        return member == null ? null : member.getUid();
    }

}
