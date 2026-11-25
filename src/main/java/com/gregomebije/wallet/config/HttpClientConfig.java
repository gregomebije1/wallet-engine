
package com.gregomebije.wallet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean("ledgerRestClient")
    public RestClient ledgerRestClient(
            RestClient.Builder builder,
            JdkClientHttpRequestFactory requestFactory,
            @Value("${services.ledger.base-url}") String baseUrl) {
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(new BufferingClientHttpRequestFactory(requestFactory))
                .requestInterceptor(new HttpLoggingInterceptor("ledger"))
                .build();
    }

    @Bean("gatewayRestClient")
    public RestClient gatewayRestClient(
            RestClient.Builder builder,
            JdkClientHttpRequestFactory requestFactory,
            @Value("${services.gateway.base-url}") String baseUrl) {
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(new BufferingClientHttpRequestFactory(requestFactory))
                .requestInterceptor(new HttpLoggingInterceptor("gateway"))
                .build();
    }

    @Bean
    public JdkClientHttpRequestFactory jdkClientHttpRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        JdkClientHttpRequestFactory factory =
                new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }
}
