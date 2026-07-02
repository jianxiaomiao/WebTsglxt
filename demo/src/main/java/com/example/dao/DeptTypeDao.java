package com.example.dao;

import com.example.entity.DeptType;
import java.util.List;

public interface DeptTypeDao {
    // 新增系别类型
    void add(DeptType deptType);
    // 修改系别类型
    void update(DeptType deptType);
    // 删除系别类型
    void delete(Integer id);
    // 查询所有系别类型
    List<DeptType> queryAll();
    // 根据ID查询
    List<DeptType> queryById(Integer id);
}