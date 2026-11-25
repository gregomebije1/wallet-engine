package com.gregomebije.wallet.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class HttpLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingInterceptor.class);

    private final String name;

    public HttpLoggingInterceptor(String name) {
        this.name = name;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        long start = System.nanoTime();

        log.debug("[{}] --> {} {}\n    headers: {}\n    body: {}",
                name, request.getMethod(), request.getURI(),
                redactHeaders(request.getHeaders()),
                redactBody(new String(body, StandardCharsets.UTF_8)));

        try {
            ClientHttpResponse response = execution.execute(request, body);
            long ms = (System.nanoTime() - start) / 1_000_000;

            String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            log.debug("[{}] <-- {} {} ({} ms)\n    body: {}",
                    name, response.getStatusCode(), request.getURI(), ms,
                    redactBody(responseBody));
            return response;
        } catch (IOException ex) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.error("[{}] <-- FAILED {} {} ({} ms): {}",
                    name, request.getMethod(), request.getURI(), ms, ex.toString());
            throw ex;
        }
    }

    private HttpHeaders redactHeaders(HttpHeaders headers) {
        HttpHeaders copy = new HttpHeaders();
        copy.putAll(headers);
        if (copy.containsHeader(HttpHeaders.AUTHORIZATION)) {
            copy.set(HttpHeaders.AUTHORIZATION, "Bearer ***");
        }
        return copy;
    }

    // Keep passwords and tokens out of your logs
    private String redactBody(String body) {
        return body.replaceAll("(?i)\"(password|accessToken|token)\"\\s*:\\s*\"[^\"]*\"", "\"$1\":\"***\"");
    }
}