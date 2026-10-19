package com.example.lostsystem.bean;

import lombok.Data;

import java.util.Date;

@Data
public class ChatHistory {
    /**
     * 聊天角色：0-用户，1-AI
     */
    private String role;

    /**
     * 聊天内容
     */
    private String content;

    /**
     * 创建时间
     */
    private Date createdDate;
}
