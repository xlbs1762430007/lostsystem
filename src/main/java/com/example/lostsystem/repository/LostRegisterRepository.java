package com.example.lostsystem.repository;

import com.example.lostsystem.entity.LostRegisterEntity;

import java.util.List;

public interface LostRegisterRepository extends BaseRepository<LostRegisterEntity>{
    /**
     * 根据手机号码查询丢失信息
     * @param phone
     * @return
     */
    List<LostRegisterEntity> findAllByPhone(String phone);
}
