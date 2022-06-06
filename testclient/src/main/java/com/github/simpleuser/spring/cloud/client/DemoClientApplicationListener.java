package com.github.simpleuser.spring.cloud.client;

import static org.springframework.cloud.config.client.ConfigClientProperties.AUTHORIZATION;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

@Component
public class DemoClientApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger                                            logger = LoggerFactory.getLogger(DemoClientApplicationListener.class);

    @Autowired
    private ApplicationContext                                             appContext;

    @Autowired
    private org.springframework.cloud.config.client.ConfigClientProperties configClientProperties;

    @Autowired
    private TestClientProperties                                           testClientProperties;

    @Autowired
    private RestTemplate                                                   restTemplate;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        downloadRequiredFilesFromConfigServer();
        terminateApplication();
    }

    private void downloadRequiredFilesFromConfigServer() {
        if (logger.isInfoEnabled()) {
            logger.info(String.format("testClientProperties : %s ", testClientProperties.getUrls()));
            logger.info("downloadFiles started");
        }

        try {
            List<CompletableFuture<Void>> downloadFilesFutures = testClientProperties.getUrls()
                                                                                     .stream()
                                                                                     .map(fileUrl -> downloadFile(fileUrl, configClientProperties.getCredentials(0)))
                                                                                     .collect(Collectors.toList());

            CompletableFuture<Void> allDownloadFilesCompletedFuture = CompletableFuture.allOf(downloadFilesFutures.toArray(new CompletableFuture[downloadFilesFutures.size()]));

            allDownloadFilesCompletedFuture.thenRun(() -> {
                if (logger.isInfoEnabled()) {
                    logger.info(String.format("downloadFiles completed for fileUrls: %s", testClientProperties.getUrls()));
                }
            }).get(testClientProperties.getDownloadFilesTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error in downloadFiles. Error: %s", ExceptionUtils.getMessage(e)), e);
        }

    }

    private CompletableFuture<Void> downloadFile(String fileUrl, ConfigClientProperties.Credentials credentials) {
        return CompletableFuture.supplyAsync(() -> {

            if (logger.isInfoEnabled()) {
                logger.info(String.format("downloading file from url: %s ", fileUrl));
            }

            restTemplate.execute(fileUrl,
                                 HttpMethod.GET,
                                 toDownloadFileRequestCallBack(credentials.getUsername(), credentials.getPassword()),
                                 toDownloadFileResponseExtractor(fileUrl));
            return null;
        });
    }

    private RequestCallback toDownloadFileRequestCallBack(String username, String password) {
        return (ClientHttpRequest request) -> {
            modifyRequestForAuthorization(request, username, password);
        };
    }

    private ResponseExtractor<Void> toDownloadFileResponseExtractor(String fileUrl) {
        return (ClientHttpResponse response) -> {
            HttpStatus statusCode = response.getStatusCode();
            try {
                if (statusCode.isError()) {
                    throw new RuntimeException(String.format("Received error response: %s ", statusCode.value()));
                }
                String remoteRelativePath = StringUtils.substringAfter(fileUrl,"testapp/common/master/testapp/");
                File target = new File(FilenameUtils.concat(testClientProperties.getTargetLocalDirPath(), remoteRelativePath));
                if (logger.isInfoEnabled()) {
                    logger.info(String.format("Writing content to target: %s ", target));
                }
                File parent = new File(target.getParent());
                if (!parent.exists()) {
                    Files.createDirectories(parent.toPath());
                }
                StreamUtils.copy(response.getBody(), new FileOutputStream(target));
            } catch (Exception e) {
                throw new RuntimeException(String.format("Error in toDownloadFileResponseExtractor. Error: %s", e.getMessage()), e);
            }
            return null;
        };
    }

    private void terminateApplication() {
        logger.info("All tasks completed successfully. Application will be terminated");
        SpringApplication.exit(appContext, () -> 0);
    }

    protected void modifyRequestForAuthorization(ClientHttpRequest request, String username, String password) {
        String authorization = configClientProperties.getHeaders().get(AUTHORIZATION);
        if (password != null && authorization != null) {
            throw new IllegalStateException("You must set either 'password' or 'authorization'");
        }

        HttpHeaders requestHeaders = request.getHeaders();
        if (password != null) {
            byte[] token = Base64Utils.encode((username + ":" + password).getBytes());
            requestHeaders.add("Authorization", "Basic " + new String(token));
        } else if (authorization != null) {
            requestHeaders.add("Authorization", authorization);
        }
    }

}
