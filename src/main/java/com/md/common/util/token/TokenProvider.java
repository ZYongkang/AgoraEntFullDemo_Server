package com.md.common.util.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenProvider {

    @Value("${agora.service.appId}")
    private String appId;

    @Value("${agora.service.appCert}")
    private String appCert;

    @Value("${agora.service.expireInSeconds:86400}")
    private int expireInSeconds;

    public String buildImToken(String chatUuid) {
        AccessToken2 accessToken = new AccessToken2(appId, appCert, expireInSeconds);
        AccessToken2.Service serviceChat = new AccessToken2.ServiceChat(chatUuid);
        serviceChat.addPrivilegeChat(AccessToken2.PrivilegeChat.PRIVILEGE_CHAT_USER,
                expireInSeconds);
        accessToken.addService(serviceChat);
        String token = null;
        try {
            token = accessToken.build();
        } catch (Exception e) {
            log.error("build im token failed | err=", e);
        }
        return token;
    }
}
