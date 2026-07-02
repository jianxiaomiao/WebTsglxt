package com.example.dao.impl;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import com.example.dao.ForumImageDao;
import com.example.entity.ForumImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.UserCommentDao;
import com.example.entity.UserComment;
import com.example.util.DBUtil;

public class UserCommentDaoImpl implements UserCommentDao {

    private final ForumImageDao forumImageDAO = new ForumImageDaoImpl();
    private static final Logger logger = LoggerFactory.getLogger(UserCommentDaoImpl.class);

    @Override
    public Integer add(UserComment userComment) {
        if (userComment == null ) {
            throw new IllegalArgumentException("用户评论不能为空");
        }
        try {
            return DBUtil.executeUpdateReturnId(
                    "insert into user_comment(UserId, UserComment, CommentTime, prefer, parent_id, reply_to_comment_id, reply_to_user_id) values (?, ?, ?, ?, ?, ?, ?)",
                    userComment.getUserid(),
                    userComment.getUserComment(),
                    userComment.getCommentTime(),
                    0,
                    userComment.getParentId() == null ? 0 : userComment.getParentId(),
                    userComment.getReplyToCommentId() == null ? 0 : userComment.getReplyToCommentId(),
                    userComment.getReplyToUserId()
            );
        } catch (SQLException e) {
            logger.error("新增用户评论失败，用户ID：{}", userComment.getUserid(), e);
            throw new RuntimeException("新增用户评论异常", e);
        }
    }

    @Override
    public void update(UserComment userComment) {
        if (userComment == null || userComment.getCommentId() == null) {
            throw new IllegalArgumentException("用户评论/评论ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update user_comment set ");
            List<Object> params = new ArrayList<>();

            if (userComment.getUserid() != null) {
                sql.append("UserId=?, ");
                params.add(userComment.getUserid());
            }
            if (userComment.getUserComment() != null) {
                sql.append("UserComment=?, ");
                params.add(userComment.getUserComment());
            }
            if (userComment.getCommentTime() != null) {
                sql.append("CommentTime=?, ");
                params.add(userComment.getCommentTime());
            }
            if (userComment.getPrefer() != null) {
                sql.append("prefer=?, ");
                params.add(userComment.getPrefer());
            }
            if (userComment.getParentId() != null) {
                sql.append("parent_id=?, ");
                params.add(userComment.getParentId());
            }
            if (userComment.getReplyToCommentId() != null) {
                sql.append("reply_to_comment_id=?, ");
                params.add(userComment.getReplyToCommentId());
            }
            if (userComment.getReplyToUserId() != null) {
                sql.append("reply_to_user_id=?, ");
                params.add(userComment.getReplyToUserId());
            }

            sql.deleteCharAt(sql.length() - 2);
            sql.append(" where CommentId=?");
            params.add(userComment.getCommentId());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("【动态更新用户评论】执行SQL: {}，参数: {}", sql, params);

            if (affectedRows == 0) {
                throw new RuntimeException("更新用户评论失败，未匹配到评论ID：" + userComment.getCommentId());
            }
        } catch (SQLException e) {
            logger.error("更新用户评论失败，评论ID：{}", userComment.getCommentId(), e);
            throw new RuntimeException("更新用户评论异常", e);
        }
    }

    @Override
    public void updatePrefer(Integer commentId, int delta) {
        try {
            DBUtil.executeUpdate(
                    "UPDATE user_comment SET prefer = prefer + ? WHERE CommentId=?",
                    delta, commentId
            );
            logger.info("更新点赞数量成功");
        } catch (SQLException e) {
            logger.error("更新点赞数异常", e);
            throw new RuntimeException("更新点赞数失败");
        }
    }

    @Override
    public void del(Integer commentId) {
        if (commentId == null) {
            throw new IllegalArgumentException("评论ID不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate("delete from user_comment where CommentId = ?", commentId);
            if (affectedRows == 0) {
                throw new RuntimeException("删除用户评论失败，未匹配到评论ID：" + commentId);
            }
        } catch (SQLException e) {
            logger.error("删除用户评论失败，评论ID：{}", commentId, e);
            throw new RuntimeException("删除用户评论异常", e);
        }
    }

    // ====================== 优化：联表查询 发布者用户名 ======================
    @Override
    public List<UserComment> queryAll() {
        try {
            List<UserComment> comments = DBUtil.executeQuery(
                    "SELECT " +
                            "uc.UserId, " +
                            "uc.UserComment, " +
                            "uc.CommentTime, " +
                            "uc.CommentId, " +
                            "uc.prefer, " +
                            "uc.parent_id, " +
                            "uc.reply_to_comment_id, " +
                            "uc.reply_to_user_id, " +
                            "uc_ui.name AS userName, " + // 发布者用户名
                            "ui.name AS reply_to_user_name " +
                            "FROM user_comment uc " +
                            "LEFT JOIN user_information uc_ui ON uc_ui.UserId = uc.UserId " + // 联表查发布者
                            "LEFT JOIN user_information ui ON ui.UserId = uc.reply_to_user_id " +
                            "ORDER BY uc.CommentTime DESC",
                    rs -> {
                        String Userid = rs.getString("UserId");
                        String UserComment = rs.getString("UserComment");
                        LocalDateTime CommentTime = rs.getTimestamp("CommentTime") != null
                                ? rs.getTimestamp("CommentTime").toLocalDateTime() : null;
                        Integer CommentId = rs.getInt("CommentId");
                        Integer prefer = rs.getInt("prefer");
                        Integer parentId = rs.getInt("parent_id");
                        Integer replyToCommentId = rs.getInt("reply_to_comment_id");
                        String replyToUserId = rs.getString("reply_to_user_id");
                        String replyToUserName = rs.getString("reply_to_user_name");
                        String userName = rs.getString("userName"); // 读取用户名

                        UserComment userComment = new UserComment(Userid, UserComment, CommentTime, CommentId);
                        userComment.setPrefer(prefer);
                        userComment.setParentId(parentId);
                        userComment.setReplyToCommentId(replyToCommentId);
                        userComment.setReplyToUserId(replyToUserId);
                        userComment.setReplyToUserName(replyToUserName);
                        userComment.setUserName(userName); // 赋值
                        return userComment;
                    });

            for (UserComment comment : comments) {
                List<ForumImage> images = forumImageDAO.queryByCommentId(comment.getCommentId());
                comment.setImages(images);
            }
            return comments;
        } catch (SQLException e) {
            logger.error("查询所有用户评论异常", e);
            return Collections.emptyList();
        }
    }

    // ====================== 优化：联表查询 发布者用户名 ======================
    @Override
    public List<UserComment> queryByCommentId(Integer commentId) {
        if (commentId == null) {
            throw new IllegalArgumentException("评论ID不能为空");
        }
        try {
            List<UserComment> comments = DBUtil.executeQuery(
                    "SELECT " +
                            "uc.UserId, " +
                            "uc.UserComment, " +
                            "uc.CommentTime, " +
                            "uc.prefer, " +
                            "uc.parent_id, " +
                            "uc.reply_to_comment_id, " +
                            "uc.reply_to_user_id, " +
                            "uc_ui.name AS userName, " +
                            "ui.name AS reply_to_user_name " +
                            "FROM user_comment uc " +
                            "LEFT JOIN user_information uc_ui ON uc_ui.UserId = uc.UserId " +
                            "LEFT JOIN user_information ui ON ui.UserId = uc.reply_to_user_id " +
                            "WHERE uc.CommentId=? ",
                    rs -> {
                        try {
                            String Userid = rs.getString("UserId");
                            String UserComment = rs.getString("UserComment");
                            LocalDateTime CommentTime = rs.getTimestamp("CommentTime") != null
                                    ? rs.getTimestamp("CommentTime").toLocalDateTime() : null;
                            Integer prefer = rs.getInt("prefer");
                            Integer parentId = rs.getInt("parent_id");
                            Integer replyToCommentId = rs.getInt("reply_to_comment_id");
                            String replyToUserId = rs.getString("reply_to_user_id");
                            String replyToUserName = rs.getString("reply_to_user_name");
                            String userName = rs.getString("userName");

                            UserComment userComment = new UserComment(Userid, UserComment, CommentTime, commentId);
                            userComment.setPrefer(prefer);
                            userComment.setParentId(parentId);
                            userComment.setReplyToCommentId(replyToCommentId);
                            userComment.setReplyToUserId(replyToUserId);
                            userComment.setReplyToUserName(replyToUserName);
                            userComment.setUserName(userName);
                            return userComment;
                        } catch (SQLException e) {
                            logger.error("解析用户评论数据异常，评论ID：{}", commentId, e);
                            throw new RuntimeException("解析用户评论数据异常", e);
                        }
                    }, commentId);

            for (UserComment comment : comments) {
                comment.setImages(forumImageDAO.queryByCommentId(comment.getCommentId()));
            }
            return comments;
        } catch (SQLException e) {
            logger.error("查询用户评论异常，评论ID：{}", commentId, e);
            return Collections.emptyList();
        }
    }

    // ====================== 优化：联表查询 发布者用户名 ======================
    @Override
    public List<UserComment> queryByUserId(String userId) {
        if (userId == null|| userId.isEmpty()) {
            throw new IllegalArgumentException("查询用户ID不能为空");
        }
        try {
            List<UserComment> comments = DBUtil.executeQuery(
                    "SELECT " +
                            "uc.CommentId, " +
                            "uc.UserComment, " +
                            "uc.CommentTime, " +
                            "uc.prefer, " +
                            "uc.parent_id, " +
                            "uc.reply_to_comment_id, " +
                            "uc.reply_to_user_id, " +
                            "uc_ui.name AS userName, " +
                            "ui.name AS reply_to_user_name " +
                            "FROM user_comment uc " +
                            "LEFT JOIN user_information uc_ui ON uc_ui.UserId = uc.UserId " +
                            "LEFT JOIN user_information ui ON ui.UserId = uc.reply_to_user_id " +
                            "WHERE uc.UserId=? ORDER BY uc.CommentTime DESC",
                    rs -> {
                        try {
                            String UserComment = rs.getString("UserComment");
                            LocalDateTime CommentTime = rs.getTimestamp("CommentTime") != null
                                    ? rs.getTimestamp("CommentTime").toLocalDateTime() : null;
                            Integer CommentId = rs.getInt("CommentId");
                            Integer prefer = rs.getInt("prefer");
                            Integer parentId = rs.getInt("parent_id");
                            Integer replyToCommentId = rs.getInt("reply_to_comment_id");
                            String replyToUserId = rs.getString("reply_to_user_id");
                            String replyToUserName = rs.getString("reply_to_user_name");
                            String userName = rs.getString("userName");

                            UserComment userComment = new UserComment(userId, UserComment, CommentTime, CommentId);
                            userComment.setPrefer(prefer);
                            userComment.setParentId(parentId);
                            userComment.setReplyToCommentId(replyToCommentId);
                            userComment.setReplyToUserId(replyToUserId);
                            userComment.setReplyToUserName(replyToUserName);
                            userComment.setUserName(userName);
                            return userComment;
                        } catch (SQLException e) {
                            logger.error("解析用户评论数据异常，UserID：{}", userId, e);
                            throw new RuntimeException("解析用户评论数据异常", e);
                        }
                    }, userId);
            for (UserComment comment : comments) {
                comment.setImages(forumImageDAO.queryByCommentId(comment.getCommentId()));
            }
            return comments;
        } catch (SQLException e) {
            logger.error("查询指定用户评论异常，UserID：{}", userId, e);
            return Collections.emptyList();
        }
    }

    // ====================== 优化：联表查询 发布者用户名 ======================
    @Override
    public List<UserComment> queryByUserIdPage(String userId, Integer pageNum, Integer pageSize) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 20) pageSize = 10;

        int offset = (pageNum - 1) * pageSize;

        try {
            List<UserComment> comments = DBUtil.executeQuery(
                    "SELECT " +
                            "uc.CommentId, " +
                            "uc.UserComment, " +
                            "uc.CommentTime, " +
                            "uc.prefer, " +
                            "uc.parent_id, " +
                            "uc.reply_to_comment_id, " +
                            "uc.reply_to_user_id, " +
                            "uc_ui.name AS userName, " +
                            "ui.name AS reply_to_user_name " +
                            "FROM user_comment uc " +
                            "LEFT JOIN user_information uc_ui ON uc_ui.UserId = uc.UserId " +
                            "LEFT JOIN user_information ui ON ui.UserId = uc.reply_to_user_id " +
                            "WHERE uc.UserId=? ORDER BY uc.CommentTime DESC LIMIT ? OFFSET ?",
                    rs -> {
                        try {
                            String UserComment = rs.getString("UserComment");
                            LocalDateTime CommentTime = rs.getTimestamp("CommentTime") != null
                                    ? rs.getTimestamp("CommentTime").toLocalDateTime() : null;
                            Integer CommentId = rs.getInt("CommentId");
                            Integer prefer = rs.getInt("prefer");
                            Integer parentId = rs.getInt("parent_id");
                            Integer replyToCommentId = rs.getInt("reply_to_comment_id");
                            String replyToUserId = rs.getString("reply_to_user_id");
                            String replyToUserName = rs.getString("reply_to_user_name");
                            String userName = rs.getString("userName");

                            UserComment userComment = new UserComment(userId, UserComment, CommentTime, CommentId);
                            userComment.setPrefer(prefer);
                            userComment.setParentId(parentId);
                            userComment.setReplyToCommentId(replyToCommentId);
                            userComment.setReplyToUserId(replyToUserId);
                            userComment.setReplyToUserName(replyToUserName);
                            userComment.setUserName(userName);
                            return userComment;
                        } catch (SQLException e) {
                            logger.error("解析用户评论数据异常", e);
                            throw new RuntimeException("解析数据异常", e);
                        }
                    }, userId, pageSize, offset);

            for (UserComment comment : comments) {
                comment.setImages(forumImageDAO.queryByCommentId(comment.getCommentId()));
            }
            return comments;
        } catch (SQLException e) {
            logger.error("分页查询用户论坛评论异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countByUserId(String userId) {
        if (userId == null || userId.isEmpty()) return 0L;
        try {
            return DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM user_comment WHERE UserId=?",
                    rs -> rs.getLong("total"),
                    userId
            ).get(0);
        } catch (SQLException e) {
            logger.error("统计用户论坛评论总数异常", e);
            return 0L;
        }
    }

    // ====================== 优化：联表查询 发布者用户名 ======================
    @Override
    public List<UserComment> queryByType(String sortType) {
        String orderBy = "ORDER BY uc.CommentTime DESC";
        if ("prefer".equals(sortType)) {
            orderBy = "ORDER BY uc.prefer DESC, uc.CommentTime DESC";
        }

        try {
            List<UserComment> comments = DBUtil.executeQuery(
                    "SELECT " +
                            "uc.CommentId, " +
                            "uc.UserId, " +
                            "uc.UserComment, " +
                            "uc.CommentTime, " +
                            "uc.prefer, " +
                            "uc.parent_id, " +
                            "uc.reply_to_comment_id, " +
                            "uc.reply_to_user_id, " +
                            "uc_ui.name AS userName, " +
                            "ui.name AS reply_to_user_name " +
                            "FROM user_comment uc " +
                            "LEFT JOIN user_information uc_ui ON uc_ui.UserId = uc.UserId " +
                            "LEFT JOIN user_information ui ON ui.UserId = uc.reply_to_user_id " +
                            orderBy,
                    rs -> {
                        try {
                            String userId = rs.getString("UserId");
                            String userComment = rs.getString("UserComment");
                            LocalDateTime commentTime = rs.getTimestamp("CommentTime") != null
                                    ? rs.getTimestamp("CommentTime").toLocalDateTime() : null;
                            Integer commentId = rs.getInt("CommentId");
                            Integer prefer = rs.getInt("prefer");
                            Integer parentId = rs.getInt("parent_id");
                            Integer replyToCommentId = rs.getInt("reply_to_comment_id");
                            String replyToUserId = rs.getString("reply_to_user_id");
                            String replyToUserName = rs.getString("reply_to_user_name");
                            String userName = rs.getString("userName");

                            UserComment comment = new UserComment(userId, userComment, commentTime, commentId);
                            comment.setPrefer(prefer);
                            comment.setParentId(parentId);
                            comment.setReplyToCommentId(replyToCommentId);
                            comment.setReplyToUserId(replyToUserId);
                            comment.setReplyToUserName(replyToUserName);
                            comment.setUserName(userName);
                            return comment;
                        } catch (SQLException e) {
                            logger.error("解析评论数据异常", e);
                            throw new RuntimeException("解析评论数据异常", e);
                        }
                    }
            );

            for (UserComment comment : comments) {
                comment.setImages(forumImageDAO.queryByCommentId(comment.getCommentId()));
            }
            return comments;
        } catch (SQLException e) {
            logger.error("按类型查询评论异常", e);
            return Collections.emptyList();
        }
    }

    // ====================== 优化：联表查询 发布者用户名 ======================
    @Override
    public List<UserComment> queryMainCommentsByPage(Integer pageNum, Integer pageSize, String sortType) {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 50) pageSize = 10;

        String orderBy = "ORDER BY uc.CommentTime DESC";
        if ("prefer".equals(sortType)) {
            orderBy = "ORDER BY uc.prefer DESC, uc.CommentTime DESC";
        }

        int offset = (pageNum - 1) * pageSize;

        try {
            List<UserComment> comments = DBUtil.executeQuery(
                    "SELECT " +
                            "uc.UserId, " +
                            "uc.UserComment, " +
                            "uc.CommentTime, " +
                            "uc.CommentId, " +
                            "uc.prefer, " +
                            "uc.parent_id, " +
                            "uc.reply_to_comment_id, " +
                            "uc.reply_to_user_id, " +
                            "uc_ui.name AS userName, " +
                            "ui.name AS reply_to_user_name " +
                            "FROM user_comment uc " +
                            "LEFT JOIN user_information uc_ui ON uc_ui.UserId = uc.UserId " +
                            "LEFT JOIN user_information ui ON ui.UserId = uc.reply_to_user_id " +
                            "WHERE uc.parent_id = 0 AND uc.status=1 " +
                            orderBy + " " +
                            "LIMIT ? OFFSET ?",
                    rs -> {
                        String Userid = rs.getString("UserId");
                        String UserComment = rs.getString("UserComment");
                        LocalDateTime CommentTime = rs.getTimestamp("CommentTime") != null
                                ? rs.getTimestamp("CommentTime").toLocalDateTime() : null;
                        Integer CommentId = rs.getInt("CommentId");
                        Integer prefer = rs.getInt("prefer");
                        Integer parentId = rs.getInt("parent_id");
                        Integer replyToCommentId = rs.getInt("reply_to_comment_id");
                        String replyToUserId = rs.getString("reply_to_user_id");
                        String replyToUserName = rs.getString("reply_to_user_name");
                        String userName = rs.getString("userName");

                        UserComment userComment = new UserComment(Userid, UserComment, CommentTime, CommentId);
                        userComment.setPrefer(prefer);
                        userComment.setParentId(parentId);
                        userComment.setReplyToCommentId(replyToCommentId);
                        userComment.setReplyToUserId(replyToUserId);
                        userComment.setReplyToUserName(replyToUserName);
                        userComment.setUserName(userName);
                        return userComment;
                    },
                    pageSize, offset
            );

            for (UserComment comment : comments) {
                List<ForumImage> images = forumImageDAO.queryByCommentId(comment.getCommentId());
                comment.setImages(images);
            }
            return comments;
        } catch (SQLException e) {
            logger.error("分页查询主评论异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countMainComments() {
        try {
            return DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM user_comment WHERE parent_id = 0",
                    rs -> rs.getLong("total")
            ).get(0);
        } catch (SQLException e) {
            logger.error("查询主评论总条数异常", e);
            return 0L;
        }
    }

    // ====================== 优化：联表查询 发布者用户名 ======================
    @Override
    public List<UserComment> querySubCommentsByPage(Integer parentId, Integer pageNum, Integer pageSize, String sortType) {
        if (parentId == null || parentId <= 0) {
            throw new IllegalArgumentException("父评论ID不能为空且必须大于0");
        }
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1 || pageSize > 20) pageSize = 3;

        String orderBy = "ORDER BY uc.CommentTime ASC";
        if ("prefer".equals(sortType)) {
            orderBy = "ORDER BY uc.prefer DESC, uc.CommentTime ASC";
        }

        int offset = (pageNum - 1) * pageSize;

        try {
            List<UserComment> comments = DBUtil.executeQuery(
                    "SELECT " +
                            "uc.UserId, " +
                            "uc.UserComment, " +
                            "uc.CommentTime, " +
                            "uc.CommentId, " +
                            "uc.prefer, " +
                            "uc.parent_id, " +
                            "uc.reply_to_comment_id, " +
                            "uc.reply_to_user_id, " +
                            "uc_ui.name AS userName, " +
                            "ui.name AS reply_to_user_name " +
                            "FROM user_comment uc " +
                            "LEFT JOIN user_information uc_ui ON uc_ui.UserId = uc.UserId " +
                            "LEFT JOIN user_information ui ON ui.UserId = uc.reply_to_user_id " +
                            "WHERE uc.parent_id = ? AND uc.status=1 " +
                            orderBy + " " +
                            "LIMIT ? OFFSET ?",
                    rs -> {
                        String Userid = rs.getString("UserId");
                        String UserComment = rs.getString("UserComment");
                        LocalDateTime CommentTime = rs.getTimestamp("CommentTime") != null
                                ? rs.getTimestamp("CommentTime").toLocalDateTime() : null;
                        Integer CommentId = rs.getInt("CommentId");
                        Integer prefer = rs.getInt("prefer");
                        Integer pId = rs.getInt("parent_id");
                        Integer replyToCommentId = rs.getInt("reply_to_comment_id");
                        String replyToUserId = rs.getString("reply_to_user_id");
                        String replyToUserName = rs.getString("reply_to_user_name");
                        String userName = rs.getString("userName");

                        UserComment userComment = new UserComment(Userid, UserComment, CommentTime, CommentId);
                        userComment.setPrefer(prefer);
                        userComment.setParentId(pId);
                        userComment.setReplyToCommentId(replyToCommentId);
                        userComment.setReplyToUserId(replyToUserId);
                        userComment.setReplyToUserName(replyToUserName);
                        userComment.setUserName(userName);
                        return userComment;
                    },
                    parentId, pageSize, offset
            );

            for (UserComment comment : comments) {
                List<ForumImage> images = forumImageDAO.queryByCommentId(comment.getCommentId());
                comment.setImages(images);
            }
            return comments;
        } catch (SQLException e) {
            logger.error("分页查询子评论异常，父评论ID：{}", parentId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Long countSubCommentsByParentId(Integer parentId) {
        if (parentId == null || parentId <= 0) {
            return 0L;
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM user_comment WHERE parent_id = ?",
                    rs -> rs.getLong("total"),
                    parentId
            ).get(0);
        } catch (SQLException e) {
            logger.error("查询子评论总条数异常，父评论ID：{}", parentId, e);
            return 0L;
        }
    }

    @Override
    public Map<Integer, Long> batchCountSubComments(List<Integer> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String placeholders = String.join(",", Collections.nCopies(parentIds.size(), "?"));

        try {
            List<Map<String, Object>> result = DBUtil.executeQuery(
                    "SELECT parent_id, COUNT(*) AS total FROM user_comment WHERE parent_id IN (" + placeholders + ") GROUP BY parent_id",
                    rs -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("parentId", rs.getInt("parent_id"));
                        map.put("total", rs.getLong("total"));
                        return map;
                    },
                    parentIds.toArray()
            );

            Map<Integer, Long> countMap = new HashMap<>();
            for (Map<String, Object> item : result) {
                Integer parentId = (Integer) item.get("parentId");
                Long total = (Long) item.get("total");
                countMap.put(parentId, total);
            }

            for (Integer parentId : parentIds) {
                countMap.putIfAbsent(parentId, 0L);
            }

            return countMap;
        } catch (SQLException e) {
            logger.error("批量统计子评论总数异常", e);
            return Collections.emptyMap();
        }
    }

    public Integer countUserCommentByUserIdAndTime(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty()) return 0;
        try {
            List<Integer> result = DBUtil.executeQuery(
                    "SELECT COUNT(*) AS total FROM user_comment WHERE UserId=? AND CommentTime BETWEEN ? AND ?",
                    rs -> rs.getInt("total"),
                    userId, startTime, endTime
            );
            return result.isEmpty() ? 0 : result.get(0);
        } catch (SQLException e) {
            logger.error("统计用户论坛评论异常", e);
            return 0;
        }
    }

    // ====================== 优化：联表查询 发布者用户名 ======================
    @Override
    public List<UserComment> queryByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        if (userId == null || userId.isEmpty() || startTime == null || endTime == null) {
            throw new IllegalArgumentException("用户ID/开始时间/结束时间不能为空");
        }
        try {
            List<UserComment> comments = DBUtil.executeQuery(
                    "SELECT " +
                            "uc.CommentId, " +
                            "uc.UserComment, " +
                            "uc.CommentTime, " +
                            "uc.prefer, " +
                            "uc.parent_id, " +
                            "uc.reply_to_comment_id, " +
                            "uc.reply_to_user_id, " +
                            "uc_ui.name AS userName, " +
                            "ui.name AS reply_to_user_name " +
                            "FROM user_comment uc " +
                            "LEFT JOIN user_information uc_ui ON uc_ui.UserId = uc.UserId " +
                            "LEFT JOIN user_information ui ON ui.UserId = uc.reply_to_user_id " +
                            "WHERE uc.UserId=? AND uc.CommentTime BETWEEN ? AND ? " +
                            "ORDER BY uc.CommentTime DESC",
                    rs -> {
                        try {
                            String UserComment = rs.getString("UserComment");
                            LocalDateTime CommentTime = rs.getTimestamp("CommentTime") != null
                                    ? rs.getTimestamp("CommentTime").toLocalDateTime() : null;
                            Integer CommentId = rs.getInt("CommentId");
                            Integer prefer = rs.getInt("prefer");
                            Integer parentId = rs.getInt("parent_id");
                            Integer replyToCommentId = rs.getInt("reply_to_comment_id");
                            String replyToUserId = rs.getString("reply_to_user_id");
                            String replyToUserName = rs.getString("reply_to_user_name");
                            String userName = rs.getString("userName");

                            UserComment userComment = new UserComment(userId, UserComment, CommentTime, CommentId);
                            userComment.setPrefer(prefer);
                            userComment.setParentId(parentId);
                            userComment.setReplyToCommentId(replyToCommentId);
                            userComment.setReplyToUserId(replyToUserId);
                            userComment.setReplyToUserName(replyToUserName);
                            userComment.setUserName(userName);
                            return userComment;
                        } catch (SQLException e) {
                            logger.error("解析时间段论坛评论数据异常，userId：{}", userId, e);
                            throw new RuntimeException("解析时间段论坛评论数据异常", e);
                        }
                    }, userId, startTime, endTime);

            for (UserComment comment : comments) {
                comment.setImages(forumImageDAO.queryByCommentId(comment.getCommentId()));
            }
            return comments;
        } catch (SQLException e) {
            logger.error("查询用户{}指定时间段论坛评论异常", userId, e);
            return Collections.emptyList();
        }
    }
}