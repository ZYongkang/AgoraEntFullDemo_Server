package com.md.common.im.config;

import lombok.Data;

@Data
public class ImProperties {

    private String appkey;

    private String clientId;

    private String clientSecret;

    private String baseUri;

    private Integer httpConnectionPoolSize;

}
