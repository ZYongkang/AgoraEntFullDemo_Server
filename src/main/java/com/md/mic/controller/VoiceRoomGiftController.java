package com.md.mic.controller;

import com.md.mic.pojos.AddGiftRequest;
import com.md.mic.pojos.AddGiftResponse;
import com.md.mic.pojos.GetGiftListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@Slf4j
@RestController
public class VoiceRoomGiftController {

    @GetMapping("/voice/room/{roomId}/gift/list")
    public GetGiftListResponse listGift(@PathVariable("roomId") String roomId,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit) {
        GetGiftListResponse response =
                new GetGiftListResponse(0, null, Collections.emptyList());
        return response;
    }

    @PostMapping("/voice/room/{roomId}/gift/add")
    public AddGiftResponse addGift(@PathVariable("roomId") String roomId,
            @RequestBody AddGiftRequest request) {
        AddGiftResponse response = new AddGiftResponse(Boolean.TRUE);
        return response;
    }
}
