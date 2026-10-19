package com.example.lostsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

@EqualsAndHashCode(callSuper = true)
@Table(name = "chat_history")
@Entity
@Data
@Comment("聊天历史")
public class ChatHistoryEntity extends BaseEntity {

    @Comment("会话id")
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    @Comment("角色：0-用户，1-AI")
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Comment("内容")
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "longtext")
    private String content;
}
