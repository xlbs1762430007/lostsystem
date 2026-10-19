package com.example.lostsystem.aop;

import com.example.lostsystem.entity.ChatHistoryEntity;
import com.example.lostsystem.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Slf4j
@Aspect
@RequiredArgsConstructor
public class ChatFlowAop {

    private static final String USER_ROLE = "0";
    private static final String AI_ROLE = "1";

    private final ChatHistoryRepository chatHistoryRepository;

    @Pointcut("@annotation(com.example.lostsystem.aop.ChatFlow)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length < 2) {
            log.warn("ChatFlow切面要求目标方法至少包含userId/sessionId和message两个参数");
            return joinPoint.proceed();
        }

        String sessionId = String.valueOf(args[0]);
        String userMessage = String.valueOf(args[1]);
        saveChatHistory(sessionId, userMessage, USER_ROLE);

        Object result = joinPoint.proceed();
        if (result instanceof Flux<?> flux) {
            StringBuilder aiMessage = new StringBuilder();
            return flux.doOnNext(item -> aiMessage.append(item == null ? "" : item))
                    .doOnComplete(() -> saveChatHistory(sessionId, aiMessage.toString(), AI_ROLE));
        }

        saveChatHistory(sessionId, result == null ? "" : result.toString(), AI_ROLE);
        return result;
    }

    private void saveChatHistory(String sessionId, String content, String role) {
        ChatHistoryEntity entity = new ChatHistoryEntity();
        entity.setSessionId(sessionId);
        entity.setContent(content);
        entity.setRole(role);
        chatHistoryRepository.save(entity);
    }
}
