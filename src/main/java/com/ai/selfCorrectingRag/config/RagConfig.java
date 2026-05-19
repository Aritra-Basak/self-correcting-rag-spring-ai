package com.ai.selfCorrectingRag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        // This builder is auto-configured by Spring Boot with your Ollama properties
        return builder.build();
    }
}