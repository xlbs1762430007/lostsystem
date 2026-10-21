package com.example.lostsystem.aiservice;

import com.example.lostsystem.aioutput.LostPropertyOutput;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

import static com.example.lostsystem.config.ChatConfig.QAQ_CHAT_BEAN;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        streamingChatModel = QAQ_CHAT_BEAN,
        tools = {"chatHistoryTools", "lostPropertyTools"})
@SystemMessage(fromResource = "registerLostProperty.txt")
public interface LostPropertyAssistant {

    @UserMessage("当前sessionId:{{sessionId}}; 用户当前消息: {{message}}")
    LostPropertyOutput registerLostProperty(@MemoryId @V("sessionId") String sessionId,
            @V("message") String message);
}
