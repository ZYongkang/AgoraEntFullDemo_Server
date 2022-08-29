package com.md.mic.common.jwt.filter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.md.mic.common.jwt.util.JwtUtil;
import com.md.mic.model.User;
import com.md.mic.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private UserService userService;

    @Override public void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        log.debug("doFilter: " + request.getRequestURL().toString());

        String token = request.getHeader("Authorization");
        if (token != null) {
            if (token.startsWith("Bearer")) {
                token = token.substring(7);
            }
            String uid = jwtUtil.getUid(token);
            if (StringUtils.isNotBlank(uid)) {
                LambdaQueryWrapper<User> queryWrapper =
                        new LambdaQueryWrapper<User>().eq(User::getUid, uid);
                User user = userService.getOne(queryWrapper);
                request.setAttribute("user", user);
            }
        } else {
            log.error("not found Authorization");
        }

        chain.doFilter(request, response);
    }
}
