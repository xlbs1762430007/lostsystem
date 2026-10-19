CREATE TABLE IF NOT EXISTS chat_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    created_date DATETIME NULL COMMENT '创建时间',
    updated_date DATETIME NULL COMMENT '更新时间',
    session_id VARCHAR(255) NOT NULL COMMENT '会话id',
    role VARCHAR(50) NOT NULL COMMENT '角色：0-用户，1-AI',
    content LONGTEXT NOT NULL COMMENT '内容',
    PRIMARY KEY (id),
    INDEX idx_chat_history_session_id (session_id),
    INDEX idx_chat_history_created_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天历史';
