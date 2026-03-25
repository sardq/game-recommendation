package com.diplome.game_recommendation.services.gigachat;

import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;
@Service
public class GigachatService {
   
    private final WebClient webClient;
    private final String accessToken;

    public GigachatService(WebClient.Builder builder, GigachatAuthService authService) throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE)
        .build();
        HttpClient httpClient = HttpClient.create()
            .secure(spec -> spec.sslContext(sslContext));
        this.webClient = WebClient.builder()
            .baseUrl("https://gigachat.devices.sberbank.ru/api/v1")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
        this.accessToken = authService.getAccessToken(); 
    }

    public String ask(String prompt) {
        Map<String, Object> requestBody = Map.of(
            "model", "GigaChat",
            "temperature", 0.2,
            "top_p", 0.9,
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );

        Map<String, Object> response = webClient.post()
            .uri("/chat/completions")
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map.class)
            .block();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        return (String) message.get("content");
    }
}