package com.md.mic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.md.mic.model.VoiceRoomUser;
import com.md.mic.pojos.PageInfo;
import com.md.mic.pojos.UserDTO;

public interface VoiceRoomUserService extends IService<VoiceRoomUser> {

    /**
     * 根据房间id删除所有房间成员
     * @param roomId
     */
    void deleteByRoomId(String roomId);

    /**
     * 根据房间id分页获取房间成员
     * @param roomId
     * @param cursor
     * @param limit
     * @return
     */
    PageInfo<UserDTO> findPageByRoomId(String roomId, String cursor, Integer limit);

    /**
     * 获取指定房间id的指定uid成员
     * @param roomId
     * @param uid
     * @return
     */
    VoiceRoomUser findByRoomIdAndUid(String roomId, String uid);

    /**
     * 添加房间成员
     * @param roomId
     * @param uid
     * @param password
     * @return
     */
    VoiceRoomUser addVoiceRoomUser(String roomId, String uid, String password);

    /**
     * 删除指定房间成员
     * @param roomId
     * @param uid
     * @param isSuccess
     */
    void deleteVoiceRoomUser(String roomId, String uid, Boolean isSuccess);

    /**
     * 踢出指定房间成员
     * @param roomId
     * @param ownerUid
     * @param kickUid
     */
    void kickVoiceRoomUser(String roomId, String ownerUid, String kickUid);
}
