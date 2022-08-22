package com.md.mic.common.jwt.filter;

import com.md.mic.common.jwt.util.VoiceRoomJwtUtil;
import com.md.mic.pojos.UserDTO;
import com.md.mic.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    private UserService userService;

    @Resource
    private VoiceRoomJwtUtil voiceRoomJwtUtil;

    @Override public void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String url = request.getRequestURL().toString();
        if (url.contains("/voice")) {
            String uid = request.getHeader("uid");
            if (StringUtils.isBlank(uid)) {
                String token = request.getHeader("Authorization");
                if (StringUtils.isNotBlank(token)) {
                    if (token.startsWith("Bearer")) {
                        token = token.substring(7);
                    }
                    uid = voiceRoomJwtUtil.getUid(token);
                }
            }
            if (StringUtils.isNotBlank(uid)) {
                UserDTO user = userService.getByUid(uid);
                request.setAttribute("user", user);
            }
        }

        chain.doFilter(request, response);
    }
}
