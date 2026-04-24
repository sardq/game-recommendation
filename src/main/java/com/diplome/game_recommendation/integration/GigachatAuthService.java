// package com.diplome.game_recommendation.integration;

// import java.util.Map;
// import java.util.UUID;

// import javax.net.ssl.SSLException;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.client.reactive.ReactorClientHttpConnector;
// import org.springframework.stereotype.Service;
// import org.springframework.util.LinkedMultiValueMap;
// import org.springframework.util.MultiValueMap;
// import org.springframework.web.reactive.function.BodyInserters;
// import org.springframework.web.reactive.function.client.WebClient;

// import io.netty.handler.ssl.SslContext;
// import io.netty.handler.ssl.SslContextBuilder;
// import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
// import reactor.netty.http.client.HttpClient;

// @Service
// public class GigachatAuthService {

//     private final WebClient webClient;
//     private final String authorizationKey; 
//     public GigachatAuthService(WebClient.Builder builder,
//                                @Value("${gigachat.authorization-key}") String authorizationKey) {
//         try {
//         SslContext sslContext = SslContextBuilder.forClient()
//             .trustManager(InsecureTrustManagerFactory.INSTANCE)
//             .build();

//         HttpClient httpClient = HttpClient.create()
//             .secure(spec -> spec.sslContext(sslContext));

//         this.webClient = builder
//             .baseUrl("https://ngw.devices.sberbank.ru:9443/api/v2")
//             .clientConnector(new ReactorClientHttpConnector(httpClient))
//             .build();
            
//     } catch (SSLException e) {
//         throw new RuntimeException("Failed to create insecure WebClient for GigaChat Auth", e);
//     }
//         this.authorizationKey = authorizationKey;
//     }

//     public String getAccessToken() {
//         MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
//         formData.add("scope", "GIGACHAT_API_PERS");

//         Map<String, Object> response = webClient.post()
//             .uri("/oauth")
//             .header("Authorization", "Basic " + authorizationKey)
//             .header("Content-Type", "application/x-www-form-urlencoded")
//             .header("Accept", "application/json")
//             .header("RqUID", UUID.randomUUID().toString()) 
//             .body(BodyInserters.fromFormData(formData))
//             .retrieve()
//             .bodyToMono(Map.class)
//             .block();

//         return (String) response.get("access_token");
//     }
// }