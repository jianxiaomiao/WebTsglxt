// com.example.dao.AiOperationLogDao
package com.example.dao;

import com.example.entity.AiOperationLog;
import java.util.List;

public interface AiOperationLogDao {
    Integer insertLog(AiOperationLog log);
    List<AiOperationLog> selectLogsByUser(String userId, int limit);
}