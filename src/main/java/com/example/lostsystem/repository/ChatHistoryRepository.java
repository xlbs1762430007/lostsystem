package com.example.lostsystem.repository;

import com.example.lostsystem.entity.ChatHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatHistoryRepository extends BaseRepository<ChatHistoryEntity> {

    /**
     * 根据sessionId查询最新的聊天记录的20条
     * @param sessionId
     * @return
     */
    List<ChatHistoryEntity> findBySessionIdOrderByCreatedDateAsc(String sessionId);

    Page<ChatHistoryEntity> findAllBySessionId(String sessionId, Pageable pageable);

    void deleteBySessionId(String sessionId);
}
