package com.md.mic.common.jwt.config;

import com.md.mic.common.jwt.util.JwtUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "jwt.token")
    public JwtProperties jwtProperties() {
        return new JwtProperties();
    }

    @Bean(name = "jwtUtils")
    public JwtUtil jwtUtils() {
        return new JwtUtil();
    }

}
