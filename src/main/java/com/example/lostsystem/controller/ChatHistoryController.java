package com.example.lostsystem.controller;

import com.example.lostsystem.bean.ChatHistory;
import com.example.lostsystem.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final AiChatService aiChatService;

    @GetMapping("/chat-history")
    public PagedModel<ChatHistory> queryChatHistory(
            @RequestParam(value = "userId", defaultValue = "111") String userId,
            @PageableDefault(direction = Sort.Direction.ASC, sort = "createdDate", size = 100) Pageable page) {
        return new PagedModel<>(aiChatService.queryChatHistory(userId, page));
    }

    @PostMapping("/clear-chat-history/{userId}")
    public void clearChatHistory(@PathVariable("userId") String userId) {
        aiChatService.clearChatHistory(userId);
    }


}
