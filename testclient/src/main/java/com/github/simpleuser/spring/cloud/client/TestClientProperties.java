package com.github.simpleuser.spring.cloud.client;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "test-client")
public class TestClientProperties {

    private List<String> urls;

    private String       targetLocalDirPath;

    private Duration     downloadFilesTimeout = Duration.ofMinutes(3);

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(List<String> urls) {
        this.urls = urls;
    }

    public String getTargetLocalDirPath() {
        return targetLocalDirPath;
    }

    public void setTargetLocalDirPath(String targetLocalDirPath) {
        this.targetLocalDirPath = targetLocalDirPath;
    }

    public Duration getDownloadFilesTimeout() {
        return downloadFilesTimeout;
    }

    public void setDownloadFilesTimeout(Duration downloadFilesTimeout) {
        this.downloadFilesTimeout = downloadFilesTimeout;
    }

}
