package com.md.mic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easemob.im.server.exception.EMInvalidArgumentException;
import com.md.common.im.ImApi;
import com.md.mic.model.EasemobUser;
import com.md.mic.model.User;
import com.md.mic.pojos.UserDTO;
import com.md.mic.repository.UserMapper;
import com.md.mic.service.EasemobUserService;
import com.md.mic.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private ImApi imApi;

    @Resource
    private EasemobUserService easemobUserService;

    @Override
    @Transactional
    public UserDTO loginDevice(String deviceId, String name, String portrait) {
        LambdaQueryWrapper<User> queryWrapper =
                new LambdaQueryWrapper<User>().eq(User::getDeviceId, deviceId);
        User user = getOne(queryWrapper);
        if (user == null) {
            String chatUid = String.format("u%s",
                    UUID.randomUUID().toString().replace("-", "")
                            .substring(1));
            user = User.create(name, deviceId, portrait);
            save(user);
            EasemobUser easemobUser = imApi.createUser(user.getUid(), chatUid);
            easemobUserService.save(easemobUser);
            return UserDTO.builder().uid(user.getUid())
                    .chatUid(easemobUser.getChatId())
                    .chatUuid(easemobUser.getChatUuid())
                    .name(user.getName())
                    .portrait(user.getPortrait())
                    .build();
        }
        String uid = user.getUid();
        EasemobUser easemobUser = easemobUserService.getOne(
                new LambdaQueryWrapper<EasemobUser>().eq(EasemobUser::getUid, uid));
        return UserDTO.builder().uid(user.getUid())
                .chatUid(easemobUser.getChatId())
                .chatUuid(easemobUser.getChatUuid())
                .name(user.getName())
                .portrait(user.getPortrait())
                .build();
    }

    @Override public Map<String, UserDTO> findByUidList(List<String> ownerUidList) {
        LambdaQueryWrapper<User> queryWrapper =
                new LambdaQueryWrapper<User>().in(User::getUid, ownerUidList);
        List<User> userList = baseMapper.selectList(queryWrapper);
        if (userList == null || userList.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, UserDTO> map = new HashMap<>(userList.size());
        for (User user : userList) {
            String uid = user.getUid();
            UserDTO userDTO = UserDTO.builder()
                    .uid(uid)
                    .portrait(user.getPortrait())
                    .name(user.getName())
                    .build();
            map.put(uid, userDTO);
        }
        return map;
    }
}
