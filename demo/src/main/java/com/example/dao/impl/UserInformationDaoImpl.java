package com.example.dao.impl;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.dao.UserInformationDao;
import com.example.entity.UserInformation;
import com.example.util.DBUtil;

public class UserInformationDaoImpl implements UserInformationDao {

    private static final Logger logger = LoggerFactory.getLogger(UserInformationDaoImpl.class);

    @Override
    public void add(UserInformation user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("用户信息/用户ID不能为空");
        }
        try {
            // 🔥 关键：入库用 DeptType（int），不用 deptTypeName
            int affectedRows = DBUtil.executeUpdate(
                    "insert into user_information(UserId, Name, Sex, Birthday, Dept_Type, Regdate, Type, Can_use, Root, Password, Salt, read_time_long, bio) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    user.getUserId(),
                    user.getName(),
                    user.getSex(),
                    user.getBirthday(),
                    user.getDeptType(), // 🔥 原来的Dept改成DeptType
                    user.getRegdate(),
                    user.getType(),
                    user.getCan_use(),
                    user.getRoot(),
                    user.getPassword(),
                    user.getSalt(),
                    user.getRead_time_long(),
                    user.getBio()
            );
            if (affectedRows == 0) {
                throw new RuntimeException("新增用户失败，受影响行数为0");
            }
        } catch (SQLException e) {
            logger.error("新增用户失败，用户ID：{}", user.getUserId(), e);
            throw new RuntimeException("新增用户异常", e);
        }
    }

    @Override
    public void update(UserInformation user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("用户信息/用户ID不能为空");
        }
        try {
            StringBuilder sql = new StringBuilder("update user_information set ");
            List<Object> params = new ArrayList<>();

            if (user.getName() != null) {
                sql.append("Name=?, ");
                params.add(user.getName());
            }
            if (user.getSex() != null) {
                sql.append("Sex=?, ");
                params.add(user.getSex());
            }
            if (user.getBirthday() != null) {
                sql.append("Birthday=?, ");
                params.add(user.getBirthday());
            }
            // 🔥 关键：更新用 DeptType（int）
            if (user.getDeptType() != null) {
                sql.append("Dept_Type=?, ");
                params.add(user.getDeptType());
            }
            if (user.getRegdate() != null) {
                sql.append("Regdate=?, ");
                params.add(user.getRegdate());
            }
            if (user.getType() != null) {
                sql.append("Type=?, ");
                params.add(user.getType());
            }
            if (user.getCan_use() != null) {
                sql.append("Can_use=?, ");
                params.add(user.getCan_use());
            }
            if (user.getRoot() != null) {
                sql.append("Root=?, ");
                params.add(user.getRoot());
            }
            if (user.getPassword() != null) {
                sql.append("Password=?, ");
                params.add(user.getPassword());
            }
            if (user.getSalt() != null) {
                sql.append("Salt=?, ");
                params.add(user.getSalt());
            }
            if (user.getRead_time_long() != null) {
                sql.append("read_time_long=?, ");
                params.add(user.getRead_time_long());
            }
            if (user.getBio() != null) {
                sql.append("bio=?, ");
                params.add(user.getBio());
            }
            sql.deleteCharAt(sql.length() - 2);
            sql.append(" where UserId=?");
            params.add(user.getUserId());

            int affectedRows = DBUtil.executeUpdate(sql.toString(), params.toArray());
            logger.info("【动态更新用户】执行SQL: {}，参数: {}", sql, params);

            if (affectedRows == 0) {
                throw new RuntimeException("更新用户失败，未匹配到用户ID：" + user.getUserId());
            }
        } catch (SQLException e) {
            logger.error("更新用户失败，用户ID：{}", user.getUserId(), e);
            throw new RuntimeException("更新用户异常", e);
        }
    }

    @Override
    public void del(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            int affectedRows = DBUtil.executeUpdate("delete from user_information where UserId = ?", id);
            if (affectedRows == 0) {
                throw new RuntimeException("删除用户失败，未匹配到用户ID：" + id);
            }
        } catch (SQLException e) {
            logger.error("删除用户失败，用户ID：{}", id, e);
            throw new RuntimeException("删除用户异常", e);
        }
    }

    // 🔥 核心：所有查询方法都改成双表联表，同时填充typeName和deptTypeName
    @Override
    public List<UserInformation> queryAll() {
        try {
            return DBUtil.executeQuery(
                    "SELECT u.*, ut.user_type AS typeName, dt.DeptType AS deptTypeName " +
                            "FROM user_information u " +
                            "LEFT JOIN user_type ut ON u.Type = ut.id " +
                            "LEFT JOIN user_dept_type dt ON u.Dept_Type = dt.id", // 🔥 新增系别表联表
                    rs -> {
                        UserInformation user = null;
                        try {
                            String UserId = rs.getString("UserId");
                            String Name = rs.getString("Name");
                            String Sex = rs.getString("Sex");
                            LocalDate Birthday = rs.getDate("Birthday") != null ? rs.getDate("Birthday").toLocalDate() : null;
                            // 🔥 读取系别ID和名称
                            Integer DeptType = rs.getInt("Dept_Type");
                            if (rs.wasNull()) DeptType = null;
                            String deptTypeName = rs.getString("deptTypeName") != null ? rs.getString("deptTypeName") : "未知";
                            LocalDate Regdate = rs.getDate("Regdate") != null ? rs.getDate("Regdate").toLocalDate() : null;
                            Integer Type = rs.getInt("Type");
                            if (rs.wasNull()) Type = null;
                            String typeName = rs.getString("typeName") != null ? rs.getString("typeName") : "未知";
                            int Can_use = rs.getInt("Can_use");
                            String Root = rs.getString("Root");
                            String Password = rs.getString("Password");
                            String Salt = rs.getString("Salt");
                            int read_time_long = rs.getInt("read_time_long");
                            // 4. ResultSet解析：读取bio并赋值
                            String bio = rs.getString("bio");
                            // 🔥 构造器参数顺序已更新，必须和实体类一致
                            user = new UserInformation(
                                    UserId, Name, Sex, Birthday,
                                    DeptType, deptTypeName, Regdate,
                                    Type, typeName,
                                    Can_use, Root, Password, Salt, read_time_long, bio
                            );
                        } catch (SQLException e) {
                            logger.error("解析用户数据异常", e);
                            throw new RuntimeException("解析用户数据异常", e);
                        }
                        return user;
                    }
            );
        } catch (SQLException e) {
            logger.error("查询所有用户异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserInformation> queryByNumber(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException("查询数量不能<=0");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT u.*, ut.user_type AS typeName, dt.DeptType AS deptTypeName " +
                            "FROM user_information u " +
                            "LEFT JOIN user_type ut ON u.Type = ut.id " +
                            "LEFT JOIN user_dept_type dt ON u.Dept_Type = dt.id " +
                            "LIMIT ?",
                    rs -> {
                        UserInformation user = null;
                        try {
                            String UserId = rs.getString("UserId");
                            String Name = rs.getString("Name");
                            String Sex = rs.getString("Sex");
                            LocalDate Birthday = rs.getDate("Birthday") != null ? rs.getDate("Birthday").toLocalDate() : null;
                            Integer DeptType = rs.getInt("Dept_Type");
                            if (rs.wasNull()) DeptType = null;
                            String deptTypeName = rs.getString("deptTypeName") != null ? rs.getString("deptTypeName") : "未知";
                            LocalDate Regdate = rs.getDate("Regdate") != null ? rs.getDate("Regdate").toLocalDate() : null;
                            Integer Type = rs.getInt("Type");
                            if (rs.wasNull()) Type = null;
                            String typeName = rs.getString("typeName") != null ? rs.getString("typeName") : "未知";
                            int Can_use = rs.getInt("Can_use");
                            String Root = rs.getString("Root");
                            String Password = rs.getString("Password");
                            String Salt = rs.getString("Salt");
                            int read_time_long = rs.getInt("read_time_long");
                            String bio = rs.getString("bio");
                            user = new UserInformation(
                                    UserId, Name, Sex, Birthday,
                                    DeptType, deptTypeName, Regdate,
                                    Type, typeName,
                                    Can_use, Root, Password, Salt, read_time_long, bio
                            );
                        } catch (SQLException e) {
                            logger.error("解析指定数量用户数据异常", e);
                            throw new RuntimeException("解析指定数量用户数据异常", e);
                        }
                        return user;
                    },
                    number
            );
        } catch (SQLException e) {
            logger.error("查询指定数量用户异常", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserInformation> queryUserById(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            return DBUtil.executeQuery(
                    "SELECT u.*, ut.user_type AS typeName, dt.DeptType AS deptTypeName " +
                            "FROM user_information u " +
                            "LEFT JOIN user_type ut ON u.Type = ut.id " +
                            "LEFT JOIN user_dept_type dt ON u.Dept_Type = dt.id " +
                            "WHERE u.UserId=?",
                    rs -> {
                        UserInformation user = null;
                        try {
                            String Name = rs.getString("Name");
                            String Sex = rs.getString("Sex");
                            LocalDate Birthday = rs.getDate("Birthday") != null ? rs.getDate("Birthday").toLocalDate() : null;
                            Integer DeptType = rs.getInt("Dept_Type");
                            if (rs.wasNull()) DeptType = null;
                            String deptTypeName = rs.getString("deptTypeName") != null ? rs.getString("deptTypeName") : "未知";
                            LocalDate Regdate = rs.getDate("Regdate") != null ? rs.getDate("Regdate").toLocalDate() : null;
                            Integer Type = rs.getInt("Type");
                            if (rs.wasNull()) Type = null;
                            String typeName = rs.getString("typeName") != null ? rs.getString("typeName") : "未知";
                            int Can_use = rs.getInt("Can_use");
                            String Root = rs.getString("Root");
                            String Password = rs.getString("Password");
                            String Salt = rs.getString("Salt");
                            int read_time_long = rs.getInt("read_time_long");
                            String bio = rs.getString("bio");
                            user = new UserInformation(
                                    id, Name, Sex, Birthday,
                                    DeptType, deptTypeName, Regdate,
                                    Type, typeName,
                                    Can_use, Root, Password, Salt, read_time_long, bio
                            );
                        } catch (SQLException e) {
                            logger.error("解析用户数据异常，用户ID：{}", id, e);
                            throw new RuntimeException("解析用户数据异常", e);
                        }
                        return user;
                    },
                    id
            );
        } catch (SQLException e) {
            logger.error("查询用户异常，用户ID：{}", id, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserInformation> queryByName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        String likeName = "%" + name + "%";
        try {
            return DBUtil.executeQuery(
                    "SELECT u.*, ut.user_type AS typeName, dt.DeptType AS deptTypeName " +
                            "FROM user_information u " +
                            "LEFT JOIN user_type ut ON u.Type = ut.id " +
                            "LEFT JOIN user_dept_type dt ON u.Dept_Type = dt.id " +
                            "WHERE u.Name LIKE ?",
                    rs -> {
                        UserInformation user = null;
                        try {
                            String UserId = rs.getString("UserId");
                            String Name = rs.getString("Name");
                            String Sex = rs.getString("Sex");
                            LocalDate Birthday = rs.getDate("Birthday") != null ? rs.getDate("Birthday").toLocalDate() : null;
                            Integer DeptType = rs.getInt("Dept_Type");
                            if (rs.wasNull()) DeptType = null;
                            String deptTypeName = rs.getString("deptTypeName") != null ? rs.getString("deptTypeName") : "未知";
                            LocalDate Regdate = rs.getDate("Regdate") != null ? rs.getDate("Regdate").toLocalDate() : null;
                            Integer Type = rs.getInt("Type");
                            if (rs.wasNull()) Type = null;
                            String typeName = rs.getString("typeName") != null ? rs.getString("typeName") : "未知";
                            int Can_use = rs.getInt("Can_use");
                            String Root = rs.getString("Root") != null ? rs.getString("Root") : null;
                            String Password = rs.getString("Password");
                            String Salt = rs.getString("Salt");
                            int read_time_long = rs.getInt("read_time_long");
                            String bio = rs.getString("bio");
                            user = new UserInformation(
                                    UserId, Name, Sex, Birthday,
                                    DeptType, deptTypeName, Regdate,
                                    Type, typeName,
                                    Can_use, Root, Password, Salt, read_time_long, bio
                            );
                        } catch (SQLException e) {
                            logger.error("解析特定姓名用户数据异常", e);
                            throw new RuntimeException("解析特定姓名用户数据异常", e);
                        }
                        return user;
                    },
                    likeName
            );
        } catch (SQLException e) {
            logger.error("查询特定姓名用户异常", e);
            return Collections.emptyList();
        }
    }

    // ====================== 新增方法1：根据 Token 查询用户 ======================
    @Override
    public UserInformation queryByToken(String token) {
        // 参数非空校验
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("登录Token不能为空");
        }
        try {
            // 联表查询：和其他查询保持一致，填充 typeName / deptTypeName
            String sql = "SELECT u.*, ut.user_type AS typeName, dt.DeptType AS deptTypeName " +
                    "FROM user_information u " +
                    "LEFT JOIN user_type ut ON u.Type = ut.id " +
                    "LEFT JOIN user_dept_type dt ON u.Dept_Type = dt.id " +
                    "WHERE u.login_token = ?";

            List<UserInformation> resultList = DBUtil.executeQuery(sql, rs -> {
                // 1. 优先初始化空对象，消除null冗余、构造参数数量报错
                UserInformation user = new UserInformation();

// 基础字段全部set赋值，不再使用多参构造
                user.setUserId(rs.getString("UserId"));
                user.setName(rs.getString("Name"));
                user.setSex(rs.getString("Sex"));
                LocalDate birthday = rs.getDate("Birthday") != null ? rs.getDate("Birthday").toLocalDate() : null;
                user.setBirthday(birthday);

                Integer deptType = rs.getInt("Dept_Type");
                if(rs.wasNull()) deptType = null;
                user.setDeptType(deptType);
                user.setDeptTypeName(rs.getString("deptTypeName") != null ? rs.getString("deptTypeName") : "未知");

                LocalDate regdate = rs.getDate("Regdate") != null ? rs.getDate("Regdate").toLocalDate() : null;
                user.setRegdate(regdate);

                Integer type = rs.getInt("Type");
                if(rs.wasNull()) type = null;
                user.setType(type);
                user.setTypeName(rs.getString("typeName") != null ? rs.getString("typeName") : "未知");

                user.setCan_use(rs.getInt("Can_use"));
                user.setRoot(rs.getString("Root"));
                user.setPassword(rs.getString("Password"));
                user.setSalt(rs.getString("Salt"));
                user.setRead_time_long(rs.getInt("read_time_long"));
                user.setBio(rs.getString("bio"));

                // 2. 新增token字段赋值
                String loginToken = rs.getString("login_token");
                user.setLogin_token(loginToken);

                // 3. 数据库DATETIME → mime4j DateTime 正确转换
                LocalDate expireDateTime = rs.getDate("token_expire") != null ? rs.getDate("token_expire").toLocalDate() : null;
                user.setToken_expire(expireDateTime);

                return user;
            }, token);

            // 列表为空返回null，否则返回第一条数据
            return resultList.isEmpty() ? null : resultList.get(0);
        } catch (SQLException e) {
            logger.error("根据Token查询用户异常，Token：{}", token, e);
            throw new RuntimeException("根据Token查询用户数据库异常", e);
        }
    }

    // ====================== 新增方法2：更新用户Token和过期时间 ======================
    @Override
    public void updateToken(String userId, String token, LocalDateTime expireTime) {
        // 参数非空校验
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        try {
            // 更新 login_token 和 token_expire 两个字段
            String sql = "UPDATE user_information SET login_token = ?, token_expire = ? WHERE UserId = ?";
            int affectedRows = DBUtil.executeUpdate(sql, token, expireTime, userId);

            if (affectedRows == 0) {
                throw new RuntimeException("更新Token失败，未匹配到用户ID：" + userId);
            }
            logger.info("用户[{}]登录Token及过期时间更新成功", userId);
        } catch (SQLException e) {
            logger.error("更新用户Token异常，用户ID：{}", userId, e);
            throw new RuntimeException("更新登录Token数据库异常", e);
        }
    }
}
