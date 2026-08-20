package com.example.lostsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@MappedSuperclass
@EntityListeners({ AuditingEntityListener.class })
@Data
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("id")
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    @Comment("创建时间")
    private Date createdDate;

    @LastModifiedDate
    @Comment("更新时间")
    private Date updatedDate;
}
