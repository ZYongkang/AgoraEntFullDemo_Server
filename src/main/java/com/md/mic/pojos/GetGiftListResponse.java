package com.md.mic.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

@Value
public class GetGiftListResponse {

    private Integer total;

    private String cursor;

    @JsonProperty("ranking_list")
    private List<GiftRecord> rankingList;

}
