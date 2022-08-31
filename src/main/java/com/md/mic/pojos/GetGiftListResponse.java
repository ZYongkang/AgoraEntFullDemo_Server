package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

@Value
public class GetGiftListResponse {

    @JsonProperty("ranking_list")
    private List<GiftRecordDTO> rankingList;

}
