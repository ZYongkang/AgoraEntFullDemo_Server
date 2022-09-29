package com.md.mic.controller;

import com.google.common.collect.Lists;
import com.md.common.util.ValidationUtil;
import com.md.mic.exception.UserNotFoundException;
import com.md.mic.model.GiftRecord;
import com.md.mic.model.VoiceRoom;
import com.md.mic.pojos.*;
import com.md.mic.pojos.vo.GiftRecordVO;
import com.md.mic.service.GiftRecordService;
import com.md.mic.service.UserService;
import com.md.mic.service.VoiceRoomMicService;
import com.md.mic.service.VoiceRoomService;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.BindingResult;
import org.springframework.validation.BindingResultUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.util.function.Tuple2;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class VoiceRoomController {

    @Resource
    private VoiceRoomService voiceRoomService;

    @Resource
    private VoiceRoomMicService voiceRoomMicService;

    @Resource
    private UserService userService;

    @Resource
    private GiftRecordService giftRecordService;

    @Value("${ranking.length:100}")
    private Integer rankingLength;

    @Resource
    private PrometheusMeterRegistry registry;

    @PostMapping("/voice/room/create")
    public CreateRoomResponse createVoiceRoom(
            @RequestBody @Validated CreateRoomRequest request, BindingResult result,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        ValidationUtil.validate(result);
        if (user == null) {
            throw new UserNotFoundException();
        }
        boolean isPrivate = Boolean.TRUE.equals(request.getIsPrivate());
        if (isPrivate && StringUtils.isEmpty(request.getPassword())) {
            throw new IllegalArgumentException("private room password must not be null!");
        }
        Instant now = Instant.now();
        Tuple2<VoiceRoomDTO, List<MicInfo>> tuples = voiceRoomService.create(user, request);
        registry.timer("create.voice.room", "result", "success")
                .record(Duration.between(now, Instant.now()));
        return new CreateRoomResponse(tuples.getT1(), tuples.getT2());
    }

    @GetMapping("/voice/room/list")
    public GetRoomListResponse getRoomList(
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(name = "type", required = false) Integer type) {
        if (limit > 100) {
            throw new IllegalArgumentException("exceeded maximum paging limit");
        }
        PageInfo<RoomListDTO> pageInfo = voiceRoomService.getByPage(cursor, limit, type);
        return new GetRoomListResponse(pageInfo.getTotal(), pageInfo.getCursor(),
                pageInfo.getList());
    }

    @GetMapping("/voice/room/{roomId}")
    public GetVoiceRoomResponse getVoiceRoomInfo(@PathVariable("roomId") String roomId,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        VoiceRoom voiceRoom = voiceRoomService.findByRoomId(roomId);
        List<GiftRecord> records =
                giftRecordService.getRankingListByRoomId(roomId, voiceRoom.getOwner(),
                        rankingLength);
        List<GiftRecordVO> list = new ArrayList<>();
        if (records != null && !records.isEmpty()) {
            ArrayList<String> uidList = records.stream().map(GiftRecord::getUid).distinct()
                    .collect(Collectors.toCollection(Lists::newArrayList));
            Map<String, UserDTO> userDTOMap = userService.findByUidList(uidList);
            list = records.stream().map(giftRecord -> {
                UserDTO dto = userDTOMap.get(giftRecord.getUid());
                return new GiftRecordVO(dto.getName(), dto.getPortrait(), giftRecord.getAmount());
            }).collect(Collectors.toList());
        }
        Long clickCount = voiceRoomService.getClickCount(roomId);
        Long memberCount = voiceRoomService.getMemberCount(roomId);
        Long giftAmount = giftRecordService.getRoomGiftAmount(roomId);
        UserDTO owner = userService.getByUid(voiceRoom.getOwner());
        VoiceRoomDTO voiceRoomDTO =
                VoiceRoomDTO.from(voiceRoom, owner, memberCount, clickCount, giftAmount);
        voiceRoomDTO = voiceRoomDTO.toBuilder()
                .rankingList(list)
                .build();
        List<MicInfo> micInfo = voiceRoomMicService.getRoomMicInfo(voiceRoom);
        return new GetVoiceRoomResponse(voiceRoomDTO, micInfo);
    }

    @PutMapping("/voice/room/{roomId}")
    public UpdateRoomInfoResponse updateVoiceRoomInfo(@PathVariable("roomId") String roomId,
            @RequestBody UpdateRoomInfoRequest request,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        voiceRoomService.updateByRoomId(roomId, request, user.getUid());
        return new UpdateRoomInfoResponse(Boolean.TRUE);
    }

    @DeleteMapping("/voice/room/{roomId}")
    public DeleteRoomResponse deleteVoiceRoom(@PathVariable("roomId") String roomId,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        if (user == null) {
            throw new UserNotFoundException();
        }
        voiceRoomService.deleteByRoomId(roomId, user.getUid());
        return new DeleteRoomResponse(Boolean.TRUE);
    }

    @PostMapping("/voice/room/{roomId}/validPassword")
    public ValidateRoomPasswordResponse validPassword(@PathVariable("roomId") String roomId,
            @RequestBody(required = true) ValidateRoomPasswordRequest request,BindingResult bindingResult,
            @RequestAttribute(name = "user", required = false) UserDTO user) {
        ValidationUtil.validate(bindingResult);
        Boolean result = voiceRoomService.validPassword(roomId, request.getPassword());
        return new ValidateRoomPasswordResponse(Boolean.TRUE.equals(result));
    }


}
