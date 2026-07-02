package com.example.service;

import com.example.dto.ResultDTO;
import com.example.entity.DeptType;
import java.util.List;

public interface DeptTypeService {
    // 查询所有系别类型
    ResultDTO<List<DeptType>> queryAllDeptTypes();
    // 根据ID查询系别类型
    ResultDTO<List<DeptType>> queryDeptTypeById(Integer id);
    // 新增系别类型
    ResultDTO<Void> addDeptType(DeptType deptType);
    // 修改系别类型
    ResultDTO<Void> updateDeptType(DeptType deptType);
    // 删除系别类型
    ResultDTO<Void> deleteDeptType(Integer id);
}