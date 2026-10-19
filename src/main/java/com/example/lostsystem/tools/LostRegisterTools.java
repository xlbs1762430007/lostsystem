package com.example.lostsystem.tools;


import com.example.lostsystem.entity.LostRegisterEntity;
import com.example.lostsystem.repository.LostRegisterRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class LostRegisterTools {
    @Autowired
    private LostRegisterRepository lostRegisterRepository;
    @Tool("根据手机号码查询丢失信息")
    public List<LostRegisterEntity> queryLostRegisterByPhone(@P("用户手机号") String phone) {
        return lostRegisterRepository.findAllByPhone(phone);
    }
}
