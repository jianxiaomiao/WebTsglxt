package com.example.dao.impl;

import com.example.dao.BookSquarePostDao;
import com.example.entity.BookSquarePost;
import com.example.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 书荒广场帖子 DAO 实现类
 * 已新增：联表用户表查询用户名 Name -> 实体 userName
 */
public class BookSquarePostDaoImpl implements BookSquarePostDao {

    private static final Logger logger = LoggerFactory.getLogger(BookSquarePostDaoImpl.class);

    @Override
    public void add(BookSquarePost post) {
        if (post == null) {
            throw new IllegalArgumentException("帖子信息不能为空");
        }
        try {
            String sql = "INSERT INTO book_square_post(parent_id, user_id, content, type, status, create_time) VALUES (?, ?, ?, ?, ?, ?)";
            int affectedRows = DBUtil.executeUpdate(sql,
                    post.getParentId(),
                    post.getUserId(),
                    post.getContent(),
                    post.getType(),
                    post.getStatus(),
                    LocalDateTime.now()
            );
            if (affectedRows == 0) {
                throw new RuntimeException("新增帖子失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增帖子异常", e);
            throw new RuntimeException("新增帖子数据库异常", e);
        }
    }

    @Override
    public void update(BookSquarePost post) {
        if (post == null || post.getId() == null) {
            throw new IllegalArgumentException("帖子/帖子ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("UPDATE book_square_post SET ");
            List<Object> params = new ArrayList<>();

            // 动态拼接：非空字段才更新
            if (post.getParentId() != null) {
                sql.append("parent_id=?, ");
                params.add(post.getParentId());
            }
            if (post.getUserId() != null) {
                sql.append("user_id=?, ");
                params.add(post.getUserId());
            }
            if (post.getContent() != null) {
                sql.append("content=?, ");
                params.add(post.getContent());
            }
            if (post.getType() != null) {
                sql.append("type=?, ");
                params.add(post.getType());
            }
            if (post.getStatus() != null) {
                sql.append("status=?, ");
                params.add(post.getStatus());
            }
            if (post.getCreateTime() != null) {
                sql.append("create_time=?, ");
                params.add(post.getCreateTime());
            }

            // 删除最后多余的逗号
            sql.deleteCharAt(sql.length() - 2);
            sql.append(" WHERE id=?");
            params.add(post.getId());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("动态更新帖子，SQL:{}", sql);

            if (affectedRows == 0) {
                throw new RuntimeException("更新帖子失败，未匹配到ID：" + post.getId());
            }
        } catch (SQLException e) {
            logger.error("更新帖子异常，ID:{}", post.getId(), e);
            throw new RuntimeException("更新帖子数据库异常", e);
        }
    }

    @Override
    public void logicDelete(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        try {
            String sql = "UPDATE book_square_post SET status=0 WHERE id=?";
            int affectedRows = DBUtil.executeUpdate(sql, id);
            if (affectedRows == 0) {
                throw new RuntimeException("逻辑删除失败，未匹配到帖子ID：" + id);
            }
        } catch (SQLException e) {
            logger.error("逻辑删除帖子异常，ID:{}", id, e);
            throw new RuntimeException("逻辑删除数据库异常", e);
        }
    }

    // ====================== 【修改】联表查询用户名 ======================
    @Override
    public BookSquarePost queryById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        try {
            // 联表 user_information 查询用户名 Name
            String sql = "SELECT p.*, u.Name " +
                    "FROM book_square_post p " +
                    "LEFT JOIN user_information u ON p.user_id = u.UserId " +
                    "WHERE p.id=? AND p.status=1";
            List<BookSquarePost> list = DBUtil.executeQuery(sql, this::buildPostEntity, id);
            return list.isEmpty() ? null : list.get(0);
        } catch (SQLException e) {
            logger.error("根据ID查询帖子异常，ID:{}", id, e);
            return null;
        }
    }

    // ====================== 分页查询实现 ======================
    // 【修改】联表用户表查询用户名
    @Override
    public List<BookSquarePost> queryAllPage(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 10;
        int offset = (pageNum - 1) * pageSize;

        try {
            String sql = "SELECT p.*, u.Name " +
                    "FROM book_square_post p " +
                    "LEFT JOIN user_information u ON p.user_id = u.UserId " +
                    "WHERE p.status=1 " +
                    "ORDER BY p.create_time DESC LIMIT ?, ?";
            return DBUtil.executeQuery(sql, this::buildPostEntity, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询所有帖子异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countAll() {
        // 统计数量无需联表，保持原逻辑
        try {
            String sql = "SELECT COUNT(*) AS total FROM book_square_post WHERE status=1";
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"));
            return res.isEmpty() ? 0L : res.get(0);
        } catch (SQLException e) {
            logger.error("统计所有帖子总数异常", e);
            return 0L;
        }
    }

    // 【修改】联表用户表查询用户名
    @Override
    public List<BookSquarePost> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 10;
        int offset = (pageNum - 1) * pageSize;

        try {
            String sql = "SELECT p.*, u.Name " +
                    "FROM book_square_post p " +
                    "LEFT JOIN user_information u ON p.user_id = u.UserId " +
                    "WHERE p.user_id=? AND p.status=1 " +
                    "ORDER BY p.create_time DESC LIMIT ?, ?";
            return DBUtil.executeQuery(sql, this::buildPostEntity, userId, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询用户帖子异常，userId:{}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByUserId(String userId) {
        // 统计数量无需联表
        if (userId == null || userId.isEmpty()) return 0L;
        try {
            String sql = "SELECT COUNT(*) AS total FROM book_square_post WHERE user_id=? AND status=1";
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), userId);
            return res.isEmpty() ? 0L : res.get(0);
        } catch (SQLException e) {
            logger.error("统计用户帖子总数异常，userId:{}", userId, e);
            return 0L;
        }
    }

    // 【修改】联表用户表 + 填充子帖数量 subPostCount
    @Override
    public List<BookSquarePost> queryMainPostPage(Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 10;
        int offset = (pageNum - 1) * pageSize;

        try {
            String sql = "SELECT p.*, u.Name " +
                    "FROM book_square_post p " +
                    "LEFT JOIN user_information u ON p.user_id = u.UserId " +
                    "WHERE p.parent_id=0 AND p.status=1 " +
                    "ORDER BY p.create_time DESC LIMIT ?, ?";
            List<BookSquarePost> postList = DBUtil.executeQuery(sql, this::buildPostEntity, offset, pageSize);

            // 遍历主帖，填充 子帖数量 subPostCount
            for (BookSquarePost post : postList) {
                Integer subCount = getSubPostCount(post.getId());
                post.setSubPostCount(subCount);
            }
            return postList;
        } catch (SQLException e) {
            logger.error("分页查询主帖异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countMainPost() {
        // 统计数量无需联表
        try {
            String sql = "SELECT COUNT(*) AS total FROM book_square_post WHERE parent_id=0 AND status=1";
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"));
            return res.isEmpty() ? 0L : res.get(0);
        } catch (SQLException e) {
            logger.error("统计主帖总数异常", e);
            return 0L;
        }
    }

    // 【修改】联表用户表查询用户名
    @Override
    public List<BookSquarePost> querySubPostByParentIdPage(Integer parentId, Integer pageNum, Integer pageSize) {
        if (parentId == null || parentId <= 0) {
            throw new IllegalArgumentException("主帖ID不合法");
        }
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 10;
        int offset = (pageNum - 1) * pageSize;

        try {
            String sql = "SELECT p.*, u.Name " +
                    "FROM book_square_post p " +
                    "LEFT JOIN user_information u ON p.user_id = u.UserId " +
                    "WHERE p.parent_id=? AND p.status=1 " +
                    "ORDER BY p.create_time ASC LIMIT ?, ?";
            return DBUtil.executeQuery(sql, this::buildPostEntity, parentId, offset, pageSize);
        } catch (SQLException e) {
            logger.error("分页查询子帖异常，parentId:{}", parentId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countSubPostByParentId(Integer parentId) {
        // 统计数量无需联表
        if (parentId == null || parentId <= 0) return 0L;
        try {
            String sql = "SELECT COUNT(*) AS total FROM book_square_post WHERE parent_id=? AND status=1";
            List<Long> res = DBUtil.executeQuery(sql, rs -> rs.getLong("total"), parentId);
            return res.isEmpty() ? 0L : res.get(0);
        } catch (SQLException e) {
            logger.error("统计子帖总数异常，parentId:{}", parentId, e);
            return 0L;
        }
    }

    @Override
    public Integer getSubPostCount(Integer parentId) {
        Long count = countSubPostByParentId(parentId);
        return count.intValue();
    }

    /**
     * 通用结果集封装实体（抽取公共代码）
     * 【核心修改】：新增读取 Name 字段，赋值给 userName
     */
    private BookSquarePost buildPostEntity(java.sql.ResultSet rs) throws SQLException {
        BookSquarePost post = new BookSquarePost();
        post.setId(rs.getInt("id"));
        post.setParentId(rs.getInt("parent_id"));
        post.setUserId(rs.getString("user_id"));
        post.setContent(rs.getString("content"));
        post.setType(rs.getInt("type"));
        post.setStatus(rs.getInt("status"));
        Timestamp time = rs.getTimestamp("create_time");
        post.setCreateTime(time != null ? time.toLocalDateTime() : null);

        // ========== 新增：读取用户表 Name 赋值给 userName ==========
        String userName = rs.getString("Name");
        post.setUserName(userName);

        return post;
    }
}