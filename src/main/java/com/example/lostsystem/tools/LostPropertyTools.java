package com.example.lostsystem.tools;

import com.example.lostsystem.entity.LostPropertyEntity;
import com.example.lostsystem.repository.LostPropertyRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LostPropertyTools {

    @Autowired
    private LostPropertyRepository lostPropertyRepository;

    @Tool("根据手机号查询物品登记信息")
    public List<LostPropertyEntity> queryLostPropertyByPhone(@P("用户手机号") String phone) {
        log.info("根据手机号查询物品登记信息，手机号：{}", phone);
        return lostPropertyRepository.findAllByPhone(phone);
    }
}
