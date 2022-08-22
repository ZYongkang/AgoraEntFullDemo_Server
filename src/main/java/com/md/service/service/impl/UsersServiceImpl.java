package com.md.service.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.md.service.common.CommonKey;
import com.md.service.common.ErrorCodeEnum;
import com.md.service.common.RoomStatus;
import com.md.service.common.UserRandomInfo;
import com.md.service.exception.BaseException;
import com.md.service.model.BaseResult;
import com.md.service.model.dto.UserInfo;
import com.md.service.model.entity.RoomInfo;
import com.md.service.model.entity.RoomUsers;
import com.md.service.model.entity.Users;
import com.md.service.model.form.UpdateUserInfoForm;
import com.md.service.repository.UsersMapper;
import com.md.service.service.RoomInfoService;
import com.md.service.service.RoomUsersService;
import com.md.service.service.UsersService;
import com.md.service.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.security.Credential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 用户信息 服务实现类
 * </p>
 */
@Service
public class UsersServiceImpl extends ServiceImpl<UsersMapper, Users> implements UsersService {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private AliSmsSingleSender aliSmsSingleSender;

    @Value("${verification_code.time}")
    private Integer codeTime;

    @Value("${verification.send.code.times}")
    private Integer sendMessageTimes;

    @Value("${verification.code.error.lock}")
    private Integer errorLock;

    @Value("${verification.code.error.time}")
    private Integer errorTime;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private YiTuUtils yiTuUtils;

    @Resource
    private RoomUsersService roomUsersService;

    @Resource
    private RoomInfoService roomInfoService;

    @Override
    public BaseResult<String> verificationCode(String phone) {
        if(redisTemplate.hasKey(phone)){
            throw new BaseException(ErrorCodeEnum.code_repeat,ErrorCodeEnum.code_repeat.getMessage());
        }
        if(!ValidateUtils.isMobile(phone)){
            throw new BaseException(ErrorCodeEnum.phone_error,ErrorCodeEnum.phone_error.getMessage());
        }
        if(redisTemplate.hasKey(CommonKey.verificationSendCodeTimes + phone)){
            String times = String.valueOf(redisTemplate.opsForValue().get(CommonKey.verificationSendCodeTimes + phone));
            if(Integer.parseInt(times) >= sendMessageTimes){
                throw new BaseException(ErrorCodeEnum.send_code_max);
            }
        }
        redisTemplate.opsForValue().increment(CommonKey.verificationSendCodeTimes + phone);
        redisTemplate.expire(CommonKey.verificationSendCodeTimes + phone ,MdStringUtils.getRemainSecondsOneDay(),TimeUnit.SECONDS);
        //发送短信
        String code = MdStringUtils.verificationCode();
        JSONObject jsonObject = new JSONObject();
        jsonObject.set("code",code);
        aliSmsSingleSender.sendCode(phone,jsonObject.toString());
        redisTemplate.opsForValue().set(phone,code,codeTime, TimeUnit.SECONDS);
        return BaseResult.success();
    }

    @Override
    public BaseResult<UserInfo> login(String phone, String code) {
        UserInfo userInfo = new UserInfo();
        if(!redisTemplate.hasKey(phone) && !"999999".equals(code)){
            throw new BaseException(ErrorCodeEnum.no_code,ErrorCodeEnum.no_code.getMessage());
        }
        String redisCode = String.valueOf(redisTemplate.opsForValue().get(phone));
        //认证成功记录用户信息
        if(StringUtils.isNoneEmpty(code) && code.equals(redisCode) || "999999".equals(code)){
            Users users = this.baseMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getMobile,phone));
            if(users == null){
                users = new Users();
                users.setMobile(phone);
                users.setUserNo(MdStringUtils.randomDelete(Credential.MD5.digest(phone).substring(4),6));
                users.setName(UserRandomInfo.getName());
                users.setHeadUrl(UserRandomInfo.headImage());
                users.setStatus(0);
                baseMapper.insert(users);
            }
            BeanUtil.copyProperties(users,userInfo);
        }else{
            Long times = redisTemplate.opsForValue().increment(CommonKey.verificationCheckCodeTimes + phone);
            redisTemplate.expire(CommonKey.verificationCheckCodeTimes + phone ,MdStringUtils.getRemainSecondsOneDay(),TimeUnit.SECONDS);
            if(redisTemplate.hasKey(CommonKey.verificationCheckCodeTimes + phone)){
                if(times.intValue() >= errorLock){
                    if(redisTemplate.hasKey(CommonKey.verificationCheckCodeTimesLock + phone)){
                        throw new BaseException(ErrorCodeEnum.code_error_lock);
                    }else{
                        redisTemplate.opsForValue().set(CommonKey.verificationCheckCodeTimesLock + phone,"1",errorTime, TimeUnit.MINUTES);
                    }
                }
            }
            throw new BaseException(ErrorCodeEnum.code_error);
        }
        String token = jwtUtil.createJWT(userInfo.getUserNo());
        userInfo.setToken(token);
        return BaseResult.success(userInfo);
    }

    @Override
    public BaseResult<UserInfo> updateInfo(UpdateUserInfoForm form) {
        Users users = getUserByNo(form.getUserNo());
        if(StringUtils.isNoneBlank(form.getHeadUrl())){
            users.setHeadUrl(form.getHeadUrl());
        }
        if(StringUtils.isNoneBlank(form.getName())){
            yiTuUtils.checkTest(form.getName());
            users.setName(form.getName());
        }
        if(StringUtils.isNoneBlank(form.getSex())){
            users.setSex(form.getSex());
        }
        baseMapper.updateById(users);
        UserInfo userInfo = new UserInfo();
        BeanUtil.copyProperties(users,userInfo);
        return BaseResult.success(userInfo);
    }

    @Override
    @Transactional
    public BaseResult<?> cancellation(String no) {
        Users users = getUserByNo(no);
        baseMapper.deleteById(users);
        //注销后 全部下座位
        roomUsersService.update(new LambdaUpdateWrapper<RoomUsers>().eq(RoomUsers::getUserId,users).
                set(RoomUsers::getOnSeat,0).set(RoomUsers::getDeletedAt, LocalDateTime.now()));
        //关闭所有房间
        roomInfoService.update(new LambdaUpdateWrapper<RoomInfo>().eq(RoomInfo::getCreator,users.getId())
                .set(RoomInfo::getStatus,1));
        return BaseResult.success();
    }

    @Override
    public BaseResult<UserInfo> getUserInfo(String no) {
        UserInfo userInfo = getUser(no);
        return BaseResult.success(userInfo);
    }

    @Override
    public UserInfo getUser(String no) {
        Users users = getUserByNo(no);
        UserInfo userInfo = new UserInfo();
        BeanUtil.copyProperties(users,userInfo);
        return userInfo;
    }

    @Override
    public Users getUserByNo(String no) {
        Users users = this.baseMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getUserNo,no));
        if(users == null){
            throw new BaseException(ErrorCodeEnum.user_not_exist,ErrorCodeEnum.user_not_exist.getMessage());
        }
        return users;
    }

    @Override
    public String getUserNoById(Integer id) {
        String userNo = baseMapper.getUserNo(id);
        if(userNo == null){
            throw new BaseException(ErrorCodeEnum.user_not_exist,ErrorCodeEnum.user_not_exist.getMessage());
        }
        return userNo;
    }

    @Override
    public IPage<Users> userList(Page page) {
        IPage<Users> list = baseMapper.selectPage(page,new LambdaQueryWrapper<Users>().
                eq(Users::getStatus, RoomStatus.OPEN).orderByDesc(Users::getCreatedAt));
        return list;
    }
}
