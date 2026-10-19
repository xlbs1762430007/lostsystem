package com.example.lostsystem.repository;

import com.example.lostsystem.entity.ChatHistoryEntity;

import java.util.List;

public interface ChatHistoryRepository extends BaseRepository<ChatHistoryEntity> {

    /**
     * 根据sessionId查询最新的聊天记录的20条
     * @param sessionId
     * @return
     */
    List<ChatHistoryEntity> findBySessionIdOrderByCreatedDateAsc(String sessionId);

    void deleteBySessionId(String sessionId);
}
