package com.james.prboard.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class WahooConfig {

    @Value("${wahoo.client-id}")
    private String clientId;

    @Value("${wahoo.redirect-uri}")
    private String redirectUri;

    @Value("${wahoo.base-url:https://api.wahooligan.com}")
    private String baseUrl;
}