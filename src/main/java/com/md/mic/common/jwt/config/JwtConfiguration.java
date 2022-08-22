package com.md.mic.common.jwt.config;

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


}
