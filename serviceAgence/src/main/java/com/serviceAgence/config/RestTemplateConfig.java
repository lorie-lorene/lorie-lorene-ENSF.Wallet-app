package com.serviceAgence.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // @Bean
    // public RestTemplate restTemplate() {
    // RestTemplate restTemplate = new RestTemplate();

    // // Configuration des timeouts
    // HttpComponentsClientHttpRequestFactory factory = new
    // HttpComponentsClientHttpRequestFactory();
    // factory.setConnectTimeout(5000); // 5 secondes
    // factory.setConnectTimeout(10000); // 10 secondes

    // restTemplate.setRequestFactory(factory);

    // // Gestion des erreurs
    // restTemplate.setErrorHandler(new ResponseErrorHandler() {
    // @Override
    // public boolean hasError(ClientHttpResponse response) throws IOException {
    // return response.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR ||
    // response.getStatusCode().series() == HttpStatus.Series.SERVER_ERROR;
    // }

    // @Override
    // public void handleError(ClientHttpResponse response) throws IOException {
    // // Log l'erreur mais ne lance pas d'exception
    // String body = StreamUtils.copyToString(response.getBody(),
    // StandardCharsets.UTF_8);
    // log.warn("Erreur HTTP {}: {}", response.getStatusCode(), body);
    // }
    // });

    // return restTemplate;
    // }
    
}