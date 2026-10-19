package com.example.lostsystem.controller;

import com.example.lostsystem.bean.ChatHistory;
import com.example.lostsystem.entity.ChatHistoryEntity;
import com.example.lostsystem.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryRepository chatHistoryRepository;

    @GetMapping("/chat-history")
    public List<ChatHistory> getChatHistory(@RequestParam("userId") String userId) {
        List<ChatHistoryEntity> entityList = chatHistoryRepository.findBySessionIdOrderByCreatedDateAsc(userId);
        List<ChatHistory> result = new ArrayList<>();
        for (ChatHistoryEntity entity : entityList) {
            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setRole(entity.getRole());
            chatHistory.setContent(entity.getContent());
            chatHistory.setCreatedDate(entity.getCreatedDate());
            result.add(chatHistory);
        }
        return result;
    }

    @DeleteMapping("/chat-history")
    public void clearChatHistory(@RequestParam("userId") String userId) {
        chatHistoryRepository.deleteBySessionId(userId);
    }
}
