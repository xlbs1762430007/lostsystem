package com.example.lostsystem.service;

import reactor.core.publisher.Flux;

public interface AiChatService {

    /**
     * 主要针对用户意图进行流程编排
     * @param message
     * @param userId
     * @return
     */
    Flux<String> chatStream(String userId, String message);


}
