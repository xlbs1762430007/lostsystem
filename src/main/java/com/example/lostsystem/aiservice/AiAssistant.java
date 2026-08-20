package com.example.lostsystem.aiservice;

import com.example.lostsystem.aioutput.IntentionOutput;
import com.example.lostsystem.aioutput.LostRegisterOutput;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        streamingChatModel = "qwenStreamingChatModel",
        tools = "testTools")
@SystemMessage(fromResource = "registerLost.txt")
public interface AiAssistant {

    String chat(@MemoryId String id, @UserMessage String message);

    Flux<String> streamingChat(@MemoryId String id, @UserMessage String message);


    @UserMessage("当前sessionId:{{sessionId}}; 用户当前消息: {{message}}")
    LostRegisterOutput registerLost(@V("sessionId") String sessionId, @V("message") String message);
}
