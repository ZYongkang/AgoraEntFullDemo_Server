package com.md.mic.pojos;

import lombok.Value;

import java.util.List;

@Value
public class GetMicApplyListResponse {

    private Long total;

    private String cursor;

    private List<MicApplyDTO> micApply;
}
