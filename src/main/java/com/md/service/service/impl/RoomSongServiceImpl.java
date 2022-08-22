package com.md.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.service.common.CommonKey;
import com.md.service.common.ErrorCodeEnum;
import com.md.service.common.SongStatus;
import com.md.service.exception.BaseException;
import com.md.service.model.dto.RoomSongInfoDTO;
import com.md.service.model.dto.RtmSongDTO;
import com.md.service.model.entity.RoomInfo;
import com.md.service.model.entity.RoomSong;
import com.md.service.model.entity.Songs;
import com.md.service.model.entity.Users;
import com.md.service.model.form.ChooseSongForm;
import com.md.service.repository.RoomSongMapper;
import com.md.service.service.*;
import com.md.service.utils.JsonUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 房间歌曲 服务实现类
 * </p>
 */
@Service
public class RoomSongServiceImpl extends ServiceImpl<RoomSongMapper, RoomSong> implements RoomSongService {

    @Resource
    private UsersService usersService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RoomUsersService roomUsersService;

//    @Resource
//    private RtmJavaClient rtmJavaClient;

    @Resource
    private RoomInfoService roomInfoService;

    @Resource
    private SongsService songsService;

    @Override
    public void chooseSong(ChooseSongForm form) {
        RLock rlock = redissonClient.getLock(form.getRoomNo());
        try {
            if(rlock.tryLock(10, TimeUnit.SECONDS)){
                usersService.getUserByNo(form.getUserNo());
                RoomSong mRoomSong = baseMapper.selectOne(new LambdaQueryWrapper<RoomSong>().
                        eq(RoomSong::getRoomNo,form.getRoomNo()).last("limit 1").orderByDesc(RoomSong::getSort));
                int sort = 1;
                if(mRoomSong != null){
                    sort = mRoomSong.getSort() + 1;
                }
                RoomSong roomSong = new RoomSong();
                roomSong.setRoomNo(form.getRoomNo());
                roomSong.setSongNo(form.getSongNo());
                roomSong.setSort(sort);
                roomSong.setUserNo(form.getUserNo());
                roomSong.setStatus(0);
                roomSong.setIsChorus(form.getIsChorus());
                baseMapper.insert(roomSong);
                RtmSongDTO rtmSongDTO = new RtmSongDTO();
                rtmSongDTO.setRoomNo(roomSong.getRoomNo());
                rtmSongDTO.setSongNo(roomSong.getSongNo());
                rtmSongDTO.setStatus(SongStatus.and_so.getCode());
//                rtmJavaClient.sendMessage(roomSong.getSongNo(), JsonUtil.toJsonString(rtmSongDTO));
                songsService.init(form.getSongName(),form.getSongUrl(),form.getSongNo(),form.getImageUrl(),form.getSinger());
            }
        } catch (InterruptedException e) {
            throw new BaseException(ErrorCodeEnum.system_error);
        }finally {
            rlock.unlock();
        }
    }

    @Override
    public void switchSong(ChooseSongForm form) {
        usersService.getUserByNo(form.getUserNo());
        RoomSong mRoomSong = baseMapper.selectOne(new LambdaQueryWrapper<RoomSong>().
                eq(RoomSong::getRoomNo,form.getRoomNo()).
                eq(RoomSong::getSongNo,form.getSongNo()).
                eq(RoomSong::getStatus,2).
                orderByAsc(RoomSong::getSort).last("limit 1"));
        if(mRoomSong == null){
            throw new BaseException(ErrorCodeEnum.no_song);
        }
        if(mRoomSong != null && mRoomSong.getIsSwitch().equals("1")){
            throw new BaseException(ErrorCodeEnum.songs_have_been_switch);
        }
        mRoomSong.setIsSwitch("1");
        mRoomSong.setStatus(1);
        baseMapper.updateById(mRoomSong);
//        RtmSongDTO rtmSongDTO = new RtmSongDTO();
//        rtmSongDTO.setRoomNo(mRoomSong.getRoomNo());
//        rtmSongDTO.setSongNo(mRoomSong.getSongNo());
//        rtmSongDTO.setStatus(SongStatus.switch_song.getCode());
//        rtmJavaClient.sendMessage(mRoomSong.getSongNo(), JsonUtil.toJsonString(rtmSongDTO));
    }

    @Override
    public void over(ChooseSongForm form) {
        usersService.getUserByNo(form.getUserNo());
        RoomSong mRoomSong = baseMapper.selectOne(new LambdaQueryWrapper<RoomSong>().
                eq(RoomSong::getRoomNo,form.getRoomNo()).
                eq(RoomSong::getSongNo,form.getSongNo()).
                eq(RoomSong::getStatus,2).
                orderByAsc(RoomSong::getScore).
                last("limit 1"));
        if(mRoomSong == null){
            throw new BaseException(ErrorCodeEnum.no_song);
        }
        if(mRoomSong != null && mRoomSong.getIsSwitch().equals("1")){
            throw new BaseException(ErrorCodeEnum.songs_have_been_switch);
        }
        mRoomSong.setStatus(1);
        mRoomSong.setScore(form.getScore());
        baseMapper.updateById(mRoomSong);
//        RtmSongDTO rtmSongDTO = new RtmSongDTO();
//        rtmSongDTO.setRoomNo(mRoomSong.getRoomNo());
//        rtmSongDTO.setSongNo(mRoomSong.getSongNo());
//        rtmSongDTO.setStatus(SongStatus.over.getCode());
//        rtmJavaClient.sendMessage(mRoomSong.getSongNo(), JsonUtil.toJsonString(rtmSongDTO));
    }

    @Override
    public void begin(ChooseSongForm form) {
        usersService.getUserByNo(form.getUserNo());
        RoomSong mRoomSong = baseMapper.selectOne(new LambdaQueryWrapper<RoomSong>().
                eq(RoomSong::getRoomNo,form.getRoomNo()).
                eq(RoomSong::getSongNo,form.getSongNo()).
                eq(RoomSong::getStatus,0).
                orderByAsc(RoomSong::getScore).
                last("limit 1"));
        if(mRoomSong == null){
            throw new BaseException(ErrorCodeEnum.no_song);
        }
        if(mRoomSong != null && mRoomSong.getIsSwitch().equals("1")){
            throw new BaseException(ErrorCodeEnum.songs_have_been_switch);
        }
        mRoomSong.setStatus(2);
        baseMapper.updateById(mRoomSong);
//        RtmSongDTO rtmSongDTO = new RtmSongDTO();
//        rtmSongDTO.setRoomNo(mRoomSong.getRoomNo());
//        rtmSongDTO.setSongNo(mRoomSong.getSongNo());
//        rtmSongDTO.setStatus(SongStatus.begin.getCode());
//        rtmJavaClient.sendMessage(mRoomSong.getSongNo(), JsonUtil.toJsonString(rtmSongDTO));
    }

    @Override
    public void chorus(ChooseSongForm form) {
        RLock rlock = redissonClient.getLock(CommonKey.on_chorus_lock + form.getRoomNo());
        try {
            if(rlock.tryLock(10,TimeUnit.SECONDS)){
                Users users =  usersService.getUserByNo(form.getUserNo());
                RoomInfo roomInfo = roomInfoService.getOne(new LambdaQueryWrapper<RoomInfo>().eq(RoomInfo::getRoomNo,form.getRoomNo()));
                if(roomInfo == null){
                    throw new BaseException(ErrorCodeEnum.no_room);
                }
                if(roomUsersService.checkStatus(form.getRoomNo(),users.getId(),0,1)){
                    roomUsersService.joinRoom(form.getRoomNo(),users.getId(),-1,1);
                    RoomSong roomSong = baseMapper.selectOne(new LambdaQueryWrapper<RoomSong>().eq(RoomSong::getRoomNo,form.getRoomNo()).
                            eq(RoomSong::getSongNo,form.getSongNo()).last("limit 1"));
                    roomSong.setChorusNo(form.getUserNo());
                    RtmSongDTO rtmSongDTO = new RtmSongDTO();
                    rtmSongDTO.setRoomNo(roomInfo.getRoomNo());
                    rtmSongDTO.setStatus(SongStatus.chorus.getCode());
//                    rtmJavaClient.sendMessage(roomInfo.getRoomNo(), JsonUtil.toJsonString(rtmSongDTO));
                }
            }
        } catch (InterruptedException e) {
            throw new BaseException(ErrorCodeEnum.system_error);
        }finally {
            rlock.unlock();
        }

    }

    @Override
    public void cancelChorus(ChooseSongForm form) {
        RLock rlock = redissonClient.getLock(form.getRoomNo());
        try {
            if(rlock.tryLock(10, TimeUnit.SECONDS)){
                RoomSong mRoomSong = baseMapper.selectOne(new LambdaQueryWrapper<RoomSong>().
                        eq(RoomSong::getRoomNo,form.getRoomNo()).
                        eq(RoomSong::getSongNo,form.getSongNo()).
                        orderByAsc(RoomSong::getSort).
                        last("limit 1"));
                mRoomSong.setIsChorus(0);
                baseMapper.updateById(mRoomSong);
            }
        }catch (Exception e){
            throw new BaseException(ErrorCodeEnum.system_error);
        }finally {
            rlock.unlock();
        }

    }

    @Override
    public List<RoomSongInfoDTO> getRoomSongInfo(String roomNo) {
        List<RoomSong> mRoomSong = baseMapper.selectList(new LambdaQueryWrapper<RoomSong>().
                eq(RoomSong::getRoomNo,roomNo).
                ne(RoomSong::getStatus,1).
                orderByAsc(RoomSong::getSort));
        List<RoomSongInfoDTO> result = new ArrayList<>();
        List<String> songNos = mRoomSong.stream().map(RoomSong::getSongNo).collect(Collectors.toList());
        List<Songs> songs = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(songNos)){
            songs = songsService.list(new LambdaQueryWrapper<Songs>().in(Songs::getSongNo,songNos));
        }
        Map<String,Songs> songMap =  songs.stream().collect(HashMap::new, (m, v)->
                m.put(v.getSongNo(), v),HashMap::putAll);
        mRoomSong.forEach(e -> {
                RoomSongInfoDTO roomSongInfoDTO = new RoomSongInfoDTO();
                roomSongInfoDTO.setSongNo(e.getSongNo());
                roomSongInfoDTO.setUserNo(e.getUserNo());
                roomSongInfoDTO.setName(usersService.getUserByNo(e.getUserNo()).getName());
                roomSongInfoDTO.setSort(e.getSort());
                roomSongInfoDTO.setIsOriginal(e.getIsOriginal());
                roomSongInfoDTO.setStatus(e.getStatus());
                if(songMap.containsKey(e.getSongNo())){
                    roomSongInfoDTO.setSongUrl(songMap.get(e.getSongNo()).getSongUrl());
                    roomSongInfoDTO.setImageUrl(songMap.get(e.getSongNo()).getImageUrl());
                    roomSongInfoDTO.setSongName(songMap.get(e.getSongNo()).getSongName());
                    roomSongInfoDTO.setSinger(songMap.get(e.getSongNo()).getSinger());
                    roomSongInfoDTO.setLyric(songMap.get(e.getSongNo()).getLyric());
                }
                roomSongInfoDTO.setChorusNo(e.getChorusNo());
                roomSongInfoDTO.setIsChorus(e.getIsChorus() == 1);
                //存在歌词返回
                if(StringUtils.isNoneBlank(roomSongInfoDTO.getLyric())){
                    result.add(roomSongInfoDTO);
                }
        });
        return result;
    }

    @Override
    public List<RoomSongInfoDTO> haveOrderedList(String roomNo) {
        return getRoomSongInfo(roomNo);
    }

    @Override
    public void toDevelop(String roomNo, String songNo,Integer sort) {
        RoomSong roomSong = baseMapper.selectOne(new LambdaQueryWrapper<RoomSong>().
                eq(RoomSong::getRoomNo,roomNo).orderByAsc(RoomSong::getSort).last("limit 1"));
        RoomSong newRoomSong = baseMapper.selectOne(new LambdaQueryWrapper<RoomSong>().
                eq(RoomSong::getRoomNo,roomNo).eq(RoomSong::getSongNo,songNo).eq(RoomSong::getSort,sort));
        Integer oldSort = roomSong.getSort();
        if(roomSong != null){// 第一个减1
            roomSong.setSort(roomSong.getSort() - 1);
            baseMapper.updateById(roomSong);
        }
        if(newRoomSong != null){// 第一位置给他
            newRoomSong.setSort(oldSort);
            baseMapper.updateById(newRoomSong);
        }
    }

    @Override
    public void delSong(String roomNo, String songNo,Integer sort) {
        RoomSong roomSong = baseMapper.selectOne(new LambdaQueryWrapper<RoomSong>().
                eq(RoomSong::getRoomNo,roomNo).
                eq(RoomSong::getSongNo,songNo).
                eq(RoomSong::getSort,sort));
        if(roomSong != null){
            baseMapper.deleteById(roomSong);
        }
    }
}
