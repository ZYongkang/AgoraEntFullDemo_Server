package com.md.mic.common.jwt.filter;

import com.md.service.model.entity.Users;
import com.md.service.service.UsersService;
import com.md.service.utils.JwtUtil;
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

    @Autowired
    private UsersService usersService;

    @Override public void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        log.debug("doFilter: " + request.getRequestURL().toString());

        String token = request.getHeader("Authorization");
        if (token != null) {
            if (token.startsWith("Bearer")) {
                token = token.substring(7);
            }
            String userNo = jwtUtil.getUserNo(token);
            if (StringUtils.isNotBlank(userNo)) {
                Users user = usersService.getUserByNo(userNo);
                request.setAttribute("user", user);
            }
        } else {
            log.error("not found Authorization");
        }

        chain.doFilter(request, response);
    }
}
