CREATE TABLE lost_property (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
    created_date DATETIME NULL COMMENT '创建时间',
    updated_date DATETIME NULL COMMENT '更新时间',
    username VARCHAR(255) NULL COMMENT '捡到失物的用户姓名',
    phone VARCHAR(255) NULL COMMENT '捡到失物的用户手机号',
    lost_name VARCHAR(255) NULL COMMENT '失物名称',
    lost_type VARCHAR(255) NULL COMMENT '失物特征',
    PRIMARY KEY (id),
    INDEX idx_lost_property_phone (phone)
) COMMENT='招领登记';
