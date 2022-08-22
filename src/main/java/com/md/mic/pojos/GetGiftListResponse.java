package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.md.mic.pojos.vo.GiftRecordVO;
import lombok.Value;

import java.util.List;

@Value
public class GetGiftListResponse {

    @JsonProperty("ranking_list")
    private List<GiftRecordVO> rankingList;

}
