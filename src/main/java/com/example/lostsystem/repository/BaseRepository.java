package com.example.lostsystem.repository;

import com.example.lostsystem.entity.BaseEntity;
import org.springframework.data.repository.CrudRepository;

public interface BaseRepository<T extends BaseEntity> extends CrudRepository<T, Long> {
}
