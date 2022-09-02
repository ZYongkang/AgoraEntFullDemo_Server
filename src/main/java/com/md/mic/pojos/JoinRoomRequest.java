package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Value;

@Value
public class JoinRoomRequest {

    private String password;

    @JsonCreator
    public JoinRoomRequest(String password) {
        this.password = password;
    }
}
