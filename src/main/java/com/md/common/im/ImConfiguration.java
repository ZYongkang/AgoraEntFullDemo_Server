package com.md.common.im;

import com.easemob.im.server.EMProperties;
import com.easemob.im.server.EMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "im.easemob")
    public ImProperties imProperties() {
        return new ImProperties();
    }

    @Bean
    public EMService emService(ImProperties imProperties) {
        EMProperties properties = EMProperties.builder()
                .setAppkey(imProperties.getAppkey())
                .setClientId(imProperties.getClientId())
                .setClientSecret(imProperties.getClientSecret())
                .build();
        return new EMService(properties);
    }

}
