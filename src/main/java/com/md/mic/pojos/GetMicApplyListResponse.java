package com.md.mic.pojos;

import com.md.mic.pojos.vo.MicApplyVO;
import lombok.Value;

import java.util.List;

@Value
public class GetMicApplyListResponse {

    private Long total;

    private String cursor;

    private List<MicApplyVO> micApply;
}
