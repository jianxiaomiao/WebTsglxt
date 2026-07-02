package com.example.dao;

import com.example.entity.ChatFile;
import java.util.List;

public interface ChatFileDao {
    // 新增文件记录
    int insert(ChatFile chatFile);
    // 根据id查询文件
    ChatFile queryById(Integer id);
    // 根据url查询（消息里存url，渲染时查详情）
    ChatFile queryByUrl(String documentUrl);
}