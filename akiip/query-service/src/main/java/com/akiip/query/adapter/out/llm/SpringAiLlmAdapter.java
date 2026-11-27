package com.akiip.query.adapter.out.llm;

import com.akiip.query.application.port.out.LlmPort;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * ADAPTER: implements LlmPort with Spring AI's ChatClient.
 * The only class that knows Spring AI exists. Which model it talks to
 * (Ollama today, Bedrock later) is decided by configuration.
 */
@Component
class SpringAiLlmAdapter implements LlmPort {

    private final ChatClient chatClient;

    SpringAiLlmAdapter(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("You are a concise assistant for software engineers. Answer in at most 5 sentences.")
                .build();
    }

    @Override
    public String ask(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}
