package com.example.lostsystem.tools;

import com.example.lostsystem.bean.ChatHistory;
import com.example.lostsystem.entity.ChatHistoryEntity;
import com.example.lostsystem.repository.ChatHistoryRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ChatHistoryTools {
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;


    @Tool(name = "获取用户聊天历史对话")
    public List<ChatHistory> getChatHistory(@P("sessionId") String sessionId) {
        List<ChatHistory> res = new ArrayList<>();
        List<ChatHistoryEntity> entityList = chatHistoryRepository.findBySessionIdOrderByCreatedDateAsc(sessionId);
        for (ChatHistoryEntity entity : entityList) {
            ChatHistory chatHistory = new ChatHistory();
            chatHistory.setRole(entity.getRole());
            chatHistory.setContent(entity.getContent());
            res.add(chatHistory);

        }
        return res;
    }
}
