package com.example.service.impl;

import com.example.dao.DeptTypeDao;
import com.example.dto.ResultDTO;
import com.example.entity.DeptType;
import com.example.service.DeptTypeService;
import com.example.util.RedisUtil;
import com.alibaba.fastjson.JSON;

import java.util.List;

public class DeptTypeServiceImpl implements DeptTypeService {

    private final DeptTypeDao deptTypeDao;
    private static final String CACHE_KEY = "dict:dept_type:all";
    private static final int CACHE_TTL = 3600;

    public DeptTypeServiceImpl(DeptTypeDao deptTypeDao) {
        this.deptTypeDao = deptTypeDao;
    }

    @Override
    public ResultDTO<List<DeptType>> queryAllDeptTypes() {
        try {
            try {
                String cached = RedisUtil.get(CACHE_KEY);
                if (cached != null) {
                    return ResultDTO.success(JSON.parseArray(cached, DeptType.class));
                }
            } catch (Exception ignored) {}

            List<DeptType> deptTypes = deptTypeDao.queryAll();
            try { RedisUtil.set(CACHE_KEY, JSON.toJSONString(deptTypes), CACHE_TTL); } catch (Exception ignored) {}
            return ResultDTO.success(deptTypes);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有系别类型失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<DeptType>> queryDeptTypeById(Integer id) {
        try {
            List<DeptType> deptTypes = deptTypeDao.queryById(id);
            return ResultDTO.success(deptTypes);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询系别类型失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addDeptType(DeptType deptType) {
        try {
            deptTypeDao.add(deptType);
            try { RedisUtil.del(CACHE_KEY); } catch (Exception ignored) {}
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增系别类型失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateDeptType(DeptType deptType) {
        try {
            deptTypeDao.update(deptType);
            try { RedisUtil.del(CACHE_KEY); } catch (Exception ignored) {}
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("修改系别类型失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteDeptType(Integer id) {
        try {
            deptTypeDao.delete(id);
            try { RedisUtil.del(CACHE_KEY); } catch (Exception ignored) {}
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除系别类型失败：" + e.getMessage());
        }
    }
}
