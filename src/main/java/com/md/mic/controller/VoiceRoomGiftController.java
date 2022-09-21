package com.md.mic.controller;

import com.md.mic.common.config.GiftId;
import com.md.mic.exception.UserNotFoundException;
import com.md.mic.model.GiftRecord;
import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.*;
import com.md.mic.pojos.vo.GiftRecordVO;
import com.md.mic.service.GiftRecordService;
import com.md.mic.service.UserService;
import com.md.mic.service.VoiceRoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class VoiceRoomGiftController {

    @Resource
    private GiftRecordService giftRecordService;

    @Resource
    private VoiceRoomService voiceRoomService;

    @Resource
    private UserService userService;

    @Value("${ranking.length:100}")
    private Integer rankingLength;

    @GetMapping("/voice/room/{roomId}/gift/list")
    public GetGiftListResponse listGift(@PathVariable("roomId") String roomId) {
        VoiceRoom voiceRoom = voiceRoomService.findByRoomId(roomId);
        List<GiftRecord> giftRecordList =
                giftRecordService.getRankingListByRoomId(roomId, voiceRoom.getOwner(),
                        rankingLength);
        if (giftRecordList == null || giftRecordList.isEmpty()) {
            return new GetGiftListResponse(Collections.emptyList());
        }
        List<String> uidList = giftRecordList.stream().map(GiftRecord::getUid)
                .collect(Collectors.toList());
        Map<String, UserDTO> userDTOMap = userService.findByUidList(uidList);
        List<GiftRecordVO> list = giftRecordList.stream().map(giftRecord -> {
            UserDTO userDTO = userDTOMap.get(giftRecord.getUid());
            return new GiftRecordVO(userDTO.getName(), userDTO.getPortrait(),
                    giftRecord.getAmount());
        }).collect(Collectors.toList());
        return new GetGiftListResponse(list);
    }

    @PostMapping("/voice/room/{roomId}/gift/add")
    public AddGiftResponse addGift(@PathVariable("roomId") String roomId,
            @RequestBody AddGiftRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        GiftId giftId = GiftId.of(request.getGiftId());
        giftRecordService.addGiftRecord(roomId, user.getUid(), giftId, request.getNum(),
                request.getToUid());
        return new AddGiftResponse(Boolean.TRUE);
    }
}
