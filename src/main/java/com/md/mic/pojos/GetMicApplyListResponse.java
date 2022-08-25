package com.md.mic.pojos;

import lombok.Value;

import java.util.List;

@Value
public class GetMicApplyListResponse {

    private Integer total;

    private String cursor;

    private List<UserDTO> members;
}
