package com.example.lostsystem.service.impl;

import com.example.lostsystem.aioutput.IntentionOutput;
import com.example.lostsystem.aioutput.LostRegisterOutput;
import com.example.lostsystem.aiservice.AiAssistant;
import com.example.lostsystem.aiservice.AiIntentionAssistant;
import com.example.lostsystem.aop.ChatFlow;
import com.example.lostsystem.entity.LostRegisterEntity;
import com.example.lostsystem.repository.LostRegisterRepository;
import com.example.lostsystem.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    @Autowired
    private AiAssistant aiAssistant;
    @Autowired
    private AiIntentionAssistant aiIntentionAssistant;
    @Autowired
    private LostRegisterRepository lostRegisterRepository;

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
}
