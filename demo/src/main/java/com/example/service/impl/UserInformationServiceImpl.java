package com.example.service.impl;

import com.example.dao.UserInformationDao;
import com.example.dto.ResultDTO;
import com.example.entity.BookInformation;
import com.example.entity.UserInformation;
import com.example.service.UserInformationService;
import com.example.util.EmojiUtils;

import java.time.LocalDateTime;
import java.util.List;

public class UserInformationServiceImpl implements UserInformationService {
    // 构造注入已有的UserInformationDao
    private final UserInformationDao userInformationDao;

    public UserInformationServiceImpl(UserInformationDao userInformationDao) {
        this.userInformationDao = userInformationDao;
    }

    @Override
    public ResultDTO<List<UserInformation>> queryAllUsers() {
        try {
            List<UserInformation> users = userInformationDao.queryAll(); // 匹配Dao层的queryall方法名
            return ResultDTO.success(users);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询所有用户失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserInformation>> queryByNumber(int number) {
        try {
            List<UserInformation> users = userInformationDao.queryByNumber(number);
            return ResultDTO.success(users);
        } catch (RuntimeException e) {
            return ResultDTO.fail("查询指定数量用户失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserInformation>> queryUserById(String userId) {
        try {
            // 校验参数非空
            if (userId == null || userId.isEmpty()) {
                return ResultDTO.paramError("用户ID必须是正整数");
            }
            List<UserInformation> users = userInformationDao.queryUserById(userId);
            return ResultDTO.success(users);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按ID查询用户失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<List<UserInformation>> queryUserByName(String name) {
        try {
            List<UserInformation> users = userInformationDao.queryByName(name);
            return ResultDTO.success(users);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("按用户名查询用户失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> addUser(UserInformation user) {
        try {
            // 基础参数校验
            if (user == null) {
                return ResultDTO.paramError("用户信息不能为空");
            }
            user.setName(EmojiUtils.addRandomEmoji(user.getName()));
            userInformationDao.add(user);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("新增用户失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> deleteUser(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return ResultDTO.paramError("用户ID必须是正整数");
            }
            userInformationDao.del(userId);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("删除用户失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateUser(UserInformation user) {
        try {
            if (user == null || user.getUserId() == null || user.getUserId().isEmpty()) {
                return ResultDTO.paramError("用户信息不能为空，且用户ID必须存在");
            }
            user.setName(EmojiUtils.addRandomEmoji(user.getName()));
            userInformationDao.update(user);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新用户失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateUserStock(UserInformation user) {
        try {
            if (user == null || user.getUserId() == null || user.getUserId().isEmpty()) {
                return ResultDTO.paramError("用户信息不能为空，且用户ID必须存在");
            }
            userInformationDao.update(user);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新用户失败：" + e.getMessage());
        }
    }

    // ====================== 【新增】Token 相关实现方法 ======================
    @Override
    public ResultDTO<UserInformation> getUserByToken(String token) {
        try {
            UserInformation user = userInformationDao.queryByToken(token);

            // 1. 判断是否查到该 Token 对应的用户
            if (user == null) {
                return ResultDTO.fail("无效的登录凭证，请重新登录");
            }

            // 2. 🔥 核心：判断 Token 是否过期
            // （注意：你在 Dao 里解析用的是 LocalDate，所以这里用 LocalDate.now() 进行比较。
            // 如果你后续为了精确到秒，把实体类改成了 LocalDateTime，这里同步改成 LocalDateTime.now() 即可）
            if (user.getToken_expire() != null && user.getToken_expire().isBefore(java.time.LocalDate.now())) {
                return ResultDTO.fail("登录已过期，请重新登录");
            }

            // 3. 验证通过，返回用户信息
            return ResultDTO.success(user);

        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("根据Token查询用户失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> updateUserToken(String userId, String token, LocalDateTime expireTime) {
        try {
            userInformationDao.updateToken(userId, token, expireTime);
            return ResultDTO.success(null);
        } catch (IllegalArgumentException e) {
            return ResultDTO.paramError(e.getMessage());
        } catch (RuntimeException e) {
            return ResultDTO.fail("更新登录Token失败：" + e.getMessage());
        }
    }
}