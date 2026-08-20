package com.example.lostsystem.tools;

import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class TestTools {
    @Tool("获取用户所在班级")
    public String getUserClass(String userName) {
        System.out.println("userName: " + userName);
//        return null;
        return "1班";
    }

    @Tool("获取今天的天气")
    public String getWeather(String address) {
        System.out.println("address: " + address);
//        return null;
        return "今天天气晴朗";
    }
}
