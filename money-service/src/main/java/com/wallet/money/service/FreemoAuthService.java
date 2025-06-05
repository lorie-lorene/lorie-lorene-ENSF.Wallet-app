package com.wallet.money.service;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FreemoAuthService {

    @Value("${freemo.api.appKey}")
    private String appKey;

    @Value("${freemo.api.secretKey}")
    private String secretKey;

    @Value("${freemo.api.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getBearerToken() {
        String url = baseUrl + "/api/v2/payment/token";

        Map<String, String> request = new HashMap<>();
        request.put("appKey", appKey);
        request.put("secretKey", secretKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String body = response.getBody();

                if (body != null) {
                    JSONObject json = new JSONObject(body);
                    if (json.has("access_token")) {
                        return json.getString("access_token");
                    } else {
                        log.error("Réponse reçue sans access_token : {}", body);
                    }
                }
            } else {
                log.error("Erreur lors de la récupération du token : {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Exception lors de la récupération du token", e);
        }

        return null;
    }

}
