package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.User;
import com.md.mic.pojos.UserDTO;

import java.util.List;
import java.util.Map;

public interface UserService extends IService<User> {

    UserDTO loginDevice(String deviceId, String name, String portrait);

    Map<String, UserDTO> findByUidList(List<String> ownerUidList);
}
