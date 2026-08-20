package com.example.lostsystem.aiservice;

import com.example.lostsystem.aioutput.IntentionOutput;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        streamingChatModel = "qwenStreamingChatModel",
        tools = "testTools")
@SystemMessage(fromResource = "getIntention.txt")
public interface AiIntentionAssistant {

    @UserMessage("当前sessionId:{{sessionId}}; 用户当前消息: {{message}}")
    IntentionOutput intention(@V("sessionId") String sessionId, @V("message") String message);
}
