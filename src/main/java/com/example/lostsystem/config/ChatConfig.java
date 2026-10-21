package com.example.lostsystem.config;

import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

    public static final String QAQ_CHAT_BEAN = "qwenQqStreamingChatModel";

    @Bean(QAQ_CHAT_BEAN)
    public StreamingChatModel qwenStreamingChatModel(
            @Value("${langchain4j.community.dashscope.streaming-chat-model.api-key}") String apiKey,
            @Value("${lostsystem.qaq.streaming-chat-model.model-name:qvq-32b}") String modelName) {
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
