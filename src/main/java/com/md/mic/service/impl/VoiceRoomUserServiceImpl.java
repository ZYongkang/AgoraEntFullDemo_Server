package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.mic.model.VoiceRoom;
import com.md.mic.model.VoiceRoomUser;
import com.md.mic.pojos.UserDTO;
import com.md.mic.repository.VoiceRoomUserMapper;
import com.md.mic.service.UserService;
import com.md.mic.service.VoiceRoomUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class VoiceRoomUserServiceImpl extends ServiceImpl<VoiceRoomUserMapper, VoiceRoomUser>
        implements VoiceRoomUserService {

    @Resource
    private UserService userService;

    @Override public List<UserDTO> getPageByRoomId(String roomId, String cursor, int limit) {
        LambdaQueryWrapper<VoiceRoomUser> queryWrapper =
                new LambdaQueryWrapper<VoiceRoomUser>().eq(VoiceRoomUser::getRoomId, roomId)
                        .orderByDesc(VoiceRoomUser::getId)
                        .last(" limit " + limit);
        List<VoiceRoomUser> voiceRoomUsers = baseMapper.selectList(queryWrapper);
        if (voiceRoomUsers == null || voiceRoomUsers.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> uidList =
                voiceRoomUsers.stream().map(VoiceRoomUser::getUid).collect(Collectors.toList());
        Map<String, UserDTO> userDTOMap = userService.findByUidList(uidList);
        return voiceRoomUsers.stream().map(voiceRoomUser -> userDTOMap.get(voiceRoomUser.getUid()))
                .collect(
                        Collectors.toList());
    }

    @Override public void deleteByRoomId(String roomId) {

    }
}
