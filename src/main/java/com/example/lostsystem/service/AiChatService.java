package com.example.lostsystem.service;

import com.example.lostsystem.bean.ChatHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;

public interface AiChatService {

    /**
     * 主要针对用户意图进行流程编排
     * @param message
     * @param userId
     * @return
     */
    Flux<String> chatStream(String userId, String message);

    /**
     * 分页查询用户聊天历史
     * @param userId 用户会话id
     * @param page 分页参数
     * @return 聊天历史分页数据
     */
    Page<ChatHistory> queryChatHistory(String userId, Pageable page);

    /**
     * 清空用户聊天历史
     * @param userId 用户会话id
     */
    void clearChatHistory(String userId);
}
