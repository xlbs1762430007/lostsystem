package com.example.lostsystem.repository;

import com.example.lostsystem.entity.LostPropertyEntity;

import java.util.List;

public interface LostPropertyRepository extends BaseRepository<LostPropertyEntity> {
    /**
     * 根据手机号查询招领登记信息
     * @param phone 用户手机号
     * @return 招领登记列表
     */
    List<LostPropertyEntity> findAllByPhone(String phone);
}
