package com.example.lostsystem.service.impl;

import com.example.lostsystem.aioutput.IntentionOutput;
import com.example.lostsystem.aioutput.LostPropertyOutput;
import com.example.lostsystem.aioutput.LostRegisterOutput;
import com.example.lostsystem.aiservice.AiAssistant;
import com.example.lostsystem.aiservice.AiIntentionAssistant;
import com.example.lostsystem.aiservice.LostPropertyAssistant;
import com.example.lostsystem.aop.ChatFlow;
import com.example.lostsystem.bean.ChatHistory;
import com.example.lostsystem.entity.ChatHistoryEntity;
import com.example.lostsystem.entity.LostPropertyEntity;
import com.example.lostsystem.entity.LostRegisterEntity;
import com.example.lostsystem.repository.ChatHistoryRepository;
import com.example.lostsystem.repository.LostPropertyRepository;
import com.example.lostsystem.repository.LostRegisterRepository;
import com.example.lostsystem.service.AiChatService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.Objects;

@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    @Autowired
    private AiAssistant aiAssistant;
    @Autowired
    private LostPropertyAssistant lostPropertyAssistant;
    @Autowired
    private AiIntentionAssistant aiIntentionAssistant;
    @Autowired
    private LostRegisterRepository lostRegisterRepository;
    @Autowired
    private LostPropertyRepository lostPropertyRepository;
    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Override
    @ChatFlow
    public Flux<String> chatStream(String userId, String message) {
        // 获取用户意图
        IntentionOutput intention = aiIntentionAssistant.intention(userId, message);

        log.info("用户意图:{}", intention);
        String output = intention.getOutput();
        switch (intention.getIntention()) {
            case 1:
                // 丢失信息登记
                output = registerLost(userId, message);
                break;
            case 2:
                // 找到失物登记
                output = registerLostProperty(userId, message);
                break;
            case 3:
                // 失物查询
                break;
            default:
                // 其他
                return Flux.just(intention.getOutput());
        }
        return Flux.just(output);
    }

    private String registerLost(String userId, String message) {
        try {
            LostRegisterOutput lostRegisterOutput = aiAssistant.registerLost(userId, message);
            log.info("丢失信息登记:{}", lostRegisterOutput);
            if (Boolean.TRUE.equals(lostRegisterOutput.getCompleted())) {
                LostRegisterEntity entity = lostRegisterOutput.getId() == null
                        ? new LostRegisterEntity()
                        : lostRegisterRepository.findById(lostRegisterOutput.getId())
                        .orElseGet(LostRegisterEntity::new);
                entity.setUsername(lostRegisterOutput.getUsername());
                entity.setPhone(lostRegisterOutput.getPhone());
                entity.setLostName(lostRegisterOutput.getLostName());
                entity.setLostType(lostRegisterOutput.getLostType());
                lostRegisterRepository.save(entity);
            }
            return lostRegisterOutput.getOutput();
        } catch (Exception e) {
            log.error("失物登记解析失败", e);
            return "登记信息处理时出现问题，请重新描述您的失物信息（姓名、手机号、物品名称及特征）。";
        }
    }

    private String registerLostProperty(String userId, String message) {
        try {
            LostPropertyOutput lostPropertyOutput = lostPropertyAssistant.registerLostProperty(userId, message);
            log.info("找到失物登记:{}", lostPropertyOutput);
            if (Boolean.TRUE.equals(lostPropertyOutput.getCompleted())) {
                LostPropertyEntity entity = resolveLostPropertyEntity(lostPropertyOutput);
                entity.setUsername(lostPropertyOutput.getUsername());
                entity.setPhone(lostPropertyOutput.getPhone());
                entity.setLostName(lostPropertyOutput.getLostName());
                entity.setLostType(lostPropertyOutput.getLostType());
                lostPropertyRepository.save(entity);
            }
            return lostPropertyOutput.getOutput();
        } catch (Exception e) {
            log.error("找到失物登记解析失败", e);
            return "招领信息处理时出现问题，请重新描述您捡到的物品信息（姓名、手机号、物品名称及特征）。";
        }
    }



    private LostPropertyEntity resolveLostPropertyEntity(LostPropertyOutput output) {
        if (output.getId() != null) {
            return lostPropertyRepository.findById(output.getId()).orElseGet(LostPropertyEntity::new);
        }
        if (isBlank(output.getPhone()) || isBlank(output.getLostName())) {
            return new LostPropertyEntity();
        }
        return lostPropertyRepository.findAllByPhone(output.getPhone()).stream()
                .filter(entity -> sameText(entity.getLostName(), output.getLostName()))
                .max(Comparator.comparing((LostPropertyEntity entity) -> isBlank(entity.getLostType()))
                        .thenComparing(LostPropertyEntity::getUpdatedDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(LostPropertyEntity::getCreatedDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseGet(LostPropertyEntity::new);
    }

    private boolean sameText(String first, String second) {
        return Objects.equals(normalize(first), normalize(second));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public Page<ChatHistory> queryChatHistory(String userId, Pageable page) {
        return chatHistoryRepository.findAllBySessionId(userId, page)
                .map(this::toChatHistory);
    }

    @Override
    @Transactional
    public void clearChatHistory(String userId) {
        chatHistoryRepository.deleteBySessionId(userId);
    }

    private ChatHistory toChatHistory(ChatHistoryEntity entity) {
        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setRole(entity.getRole());
        chatHistory.setContent(entity.getContent());
        chatHistory.setCreatedDate(entity.getCreatedDate());
        return chatHistory;
    }
}
