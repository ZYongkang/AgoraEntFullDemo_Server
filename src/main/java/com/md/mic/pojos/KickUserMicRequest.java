package com.md.mic.pojos;

import lombok.Value;

@Value
public class KickUserMicRequest {

    private Integer index;

    private String uid;
}
