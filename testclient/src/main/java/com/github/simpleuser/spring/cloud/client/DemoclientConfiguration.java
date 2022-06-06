package com.github.simpleuser.spring.cloud.client;

import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class DemoclientConfiguration {

    @Bean
    public RestTemplate restTemplate(ConfigClientProperties configClientProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        if (configClientProperties.getRequestReadTimeout() < 0) {
            throw new IllegalArgumentException("Invalid Value for Read Timeout set.");
        }
        if (configClientProperties.getRequestConnectTimeout() < 0) {
            throw new IllegalArgumentException("Invalid Value for Connect Timeout set.");
        }
        requestFactory.setReadTimeout(configClientProperties.getRequestReadTimeout());
        requestFactory.setConnectTimeout(configClientProperties.getRequestConnectTimeout());
        return new RestTemplate(requestFactory);
    }

}
