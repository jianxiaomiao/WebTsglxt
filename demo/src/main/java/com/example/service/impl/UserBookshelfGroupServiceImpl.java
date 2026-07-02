package com.example.service.impl;

import com.example.dao.UserBookshelfGroupDao;
import com.example.dao.UserCollectionDao;
import com.example.dto.ResultDTO;
import com.example.entity.UserBookshelfGroup;
import com.example.service.UserBookshelfGroupService;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserBookshelfGroupServiceImpl implements UserBookshelfGroupService {
    private final UserBookshelfGroupDao groupDao;
    private final UserCollectionDao collectionDao;

    private static final Logger logger = LoggerFactory.getLogger(UserBookshelfGroupServiceImpl.class);
    // 构造器双注入
    public UserBookshelfGroupServiceImpl(UserBookshelfGroupDao groupDao, UserCollectionDao collectionDao) {
        this.groupDao = groupDao;
        this.collectionDao = collectionDao;
    }

    @Override
    public ResultDTO<Void> addGroup(UserBookshelfGroup group) {
        try {
            groupDao.add(group);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增分组失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateGroup(UserBookshelfGroup group) {
        try {
            groupDao.update(group);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新分组失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteGroup(Integer groupId) {
        try {
            // 1. 查询分组，获取所属用户UserId
            List<UserBookshelfGroup> targetGroupList = groupDao.queryById(groupId);
            UserBookshelfGroup targetGroup = targetGroupList.getFirst();
            if (targetGroup == null) {
                return ResultDTO.paramError("目标分组不存在");
            }
            String targetUserId = targetGroup.getUserId();

            // 2. 开启事务，两条SQL原子执行：清空书籍分组 + 删除分组
            DBUtil.executeTransaction(conn -> {
                // ① 批量更新：该用户下所有此分组的收藏 GroupId 置 null
                String updateSql = "UPDATE user_collection SET GroupId = NULL WHERE UserId = ? AND GroupId = ?";
                int updateRows = 0;
                try {
                    updateRows = DBUtil.executeUpdateWithConn(conn, updateSql, targetUserId, groupId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                logger.info("事务-清空收藏分组，影响行数：{}", updateRows);

                // ② 删除分组本身
                String deleteSql = "DELETE FROM user_bookshelf_group WHERE GroupId = ?";
                int deleteRows = 0;
                try {
                    deleteRows = DBUtil.executeUpdateWithConn(conn, deleteSql, groupId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                if (deleteRows == 0) {
                    throw new RuntimeException("未匹配到待删除分组ID：" + groupId);
                }
                logger.info("事务-删除分组成功 groupId:{}", groupId);
            });

            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (SQLException e) {
            return ResultDTO.fail("删除分组事务失败：" + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("删除分组业务异常 groupId:{}", groupId, e);
            return ResultDTO.fail("删除分组失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserBookshelfGroup>> queryGroupById(Integer groupId) {
        try {
            List<UserBookshelfGroup> list = groupDao.queryById(groupId);
            return ResultDTO.success(list);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询分组失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Map<String, Object>> queryGroupByUserId(String userId, Integer page, Integer pageSize) {
        try {
            List<UserBookshelfGroup> list = groupDao.queryByUserId(userId, page, pageSize);
            int total = groupDao.countByUserId(userId);

            Map<String, Object> data = new HashMap<>();
            data.put("list", list);
            data.put("total", total);
            data.put("page", page);
            data.put("pageSize", pageSize);
            return ResultDTO.success(data);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询用户分组失败：" + e.getMessage());
        }
    }
}