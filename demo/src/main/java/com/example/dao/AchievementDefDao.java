package com.example.dao;

import com.example.entity.AchievementDef;
import java.util.List;

public interface AchievementDefDao {
    /** 查询所有成就定义 */
    List<AchievementDef> queryAll();
    /** 根据ID查询 */
    AchievementDef queryById(String id);
}
