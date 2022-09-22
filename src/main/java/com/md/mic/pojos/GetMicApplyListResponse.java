package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.md.mic.pojos.vo.MicApplyVO;
import lombok.Value;

import java.util.List;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetMicApplyListResponse {

    private Long total;

    private String cursor;

    @JsonProperty("apply_list")
    private List<MicApplyVO> micApply;
}
