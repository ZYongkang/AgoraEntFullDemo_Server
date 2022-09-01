package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.mic.exception.VoiceRoomSecurityException;
import com.md.mic.model.User;
import com.md.mic.model.VoiceRoom;
import com.md.mic.model.VoiceRoomUser;
import com.md.mic.pojos.PageInfo;
import com.md.mic.pojos.UserDTO;
import com.md.mic.pojos.VoiceRoomDTO;
import com.md.mic.repository.VoiceRoomUserMapper;
import com.md.mic.service.UserService;
import com.md.mic.service.VoiceRoomService;
import com.md.mic.service.VoiceRoomUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VoiceRoomUserServiceImpl extends ServiceImpl<VoiceRoomUserMapper, VoiceRoomUser>
        implements VoiceRoomUserService {

    @Resource
    private UserService userService;

    @Resource
    private VoiceRoomService voiceRoomService;

    @Override public void deleteByRoomId(String roomId) {
        LambdaQueryWrapper<VoiceRoomUser> queryWrapper =
                new LambdaQueryWrapper<VoiceRoomUser>().eq(VoiceRoomUser::getRoomId, roomId);
        List<VoiceRoomUser> voiceRoomUserList = baseMapper.selectList(queryWrapper);
        List<Integer> idList =
                voiceRoomUserList.stream().map(VoiceRoomUser::getId).collect(Collectors.toList());
        baseMapper.deleteBatchIds(idList);
    }

    @Override
    public PageInfo<UserDTO> findPageByRoomId(String roomId, String cursor, Integer limit) {
        Long total = baseMapper.selectCount(new LambdaQueryWrapper<>());
        int limitSize = limit + 1;
        List<VoiceRoomUser> voiceRoomUserList;
        if (StringUtils.isBlank(cursor)) {
            LambdaQueryWrapper<VoiceRoomUser> queryWrapper =
                    new LambdaQueryWrapper<VoiceRoomUser>().orderByDesc(VoiceRoomUser::getId)
                            .last(" limit " + limitSize);
            voiceRoomUserList = baseMapper.selectList(queryWrapper);
        } else {
            String s = new String(
                    Base64.getUrlDecoder().decode(cursor.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
            int id = Integer.parseInt(s);
            LambdaQueryWrapper<VoiceRoomUser> queryWrapper =
                    new LambdaQueryWrapper<VoiceRoomUser>().le(VoiceRoomUser::getId, id)
                            .orderByDesc(VoiceRoomUser::getId)
                            .last(" limit " + limitSize);
            voiceRoomUserList = baseMapper.selectList(queryWrapper);
        }

        if (voiceRoomUserList == null || voiceRoomUserList.isEmpty()) {
            return new PageInfo<>();
        }

        if (voiceRoomUserList.size() == limitSize) {
            VoiceRoomUser voiceRoomUser = voiceRoomUserList.get(limitSize - 1);
            Integer id = voiceRoomUser.getId();
            cursor = Base64.getUrlEncoder()
                    .encodeToString(String.valueOf(id).getBytes(StandardCharsets.UTF_8));
            voiceRoomUserList.remove(voiceRoomUser);
        } else {
            cursor = null;
        }
        List<String> uidList =
                voiceRoomUserList.stream().map(VoiceRoomUser::getUid).collect(Collectors.toList());
        Map<String, UserDTO> userDTOMap = userService.findByUidList(uidList);
        List<UserDTO> userDTOList = voiceRoomUserList.stream()
                .map(voiceRoomUser -> userDTOMap.get(voiceRoomUser.getUid()))
                .collect(Collectors.toList());
        PageInfo<UserDTO> pageInfo = new PageInfo<>();
        pageInfo.setCursor(cursor);
        pageInfo.setTotal(total);
        pageInfo.setList(userDTOList);
        return pageInfo;
    }

    @Override public VoiceRoomUser findByRoomIdAndUid(String roomId, String uid) {
        LambdaQueryWrapper<VoiceRoomUser> queryWrapper =
                new LambdaQueryWrapper<VoiceRoomUser>().eq(VoiceRoomUser::getRoomId, roomId)
                        .eq(VoiceRoomUser::getUid, uid);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override public VoiceRoomUser addVoiceRoomUser(String roomId, User user, String password) {
        VoiceRoom voiceRoom = voiceRoomService.findByRoomId(roomId);
        if (Boolean.TRUE.equals(voiceRoom.getIsPrivate()) && !voiceRoom.getPassword().equals(password)) {
            throw new VoiceRoomSecurityException("wrong password");
        }
        VoiceRoomUser voiceRoomUser = VoiceRoomUser.create(roomId, user.getUid());
        save(voiceRoomUser);
        return voiceRoomUser;
    }

}
