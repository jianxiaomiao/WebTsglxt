package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.dao.impl.UserInformationDaoImpl;
import com.example.dto.ResultDTO;
import com.example.entity.UserInformation;
import com.example.service.UserInformationService;
import com.example.service.impl.UserInformationServiceImpl;
import com.example.util.EmojiUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/user/info")
public class UserInformationServlet extends BaseServlet {
    private UserInformationDaoImpl userInformationDao;
    private UserInformationService userInformationService;
    private static final Logger logger = LoggerFactory.getLogger(UserInformationServlet.class);
    // 时间格式化器（解析前端传入的过期时间）
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void init() throws ServletException {
        userInformationDao = new UserInformationDaoImpl();
        userInformationService = new UserInformationServiceImpl(userInformationDao);
    }

    // ========== doGet：新增 token 查询分支，原有逻辑保留 ==========
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String userIdStr = req.getParameter("userId");
        String userName = req.getParameter("name");
        // 【新增】获取 token 参数
        String token = req.getParameter("token");

        try {
            ResultDTO<?> result;
            // 优先级：Token查询 > 用户ID查询 > 用户名查询 > 查询全部
            if (token != null && !token.isEmpty()) {
                // 【新增】根据Token查询用户（自动登录场景）
                result = userInformationService.getUserByToken(token);
                logger.info("根据Token[{}]查询用户完成", token);
            } else if (userIdStr != null && !userIdStr.isEmpty()) {
                // 按ID查询（原有逻辑）
                result = userInformationService.queryUserById(userIdStr);
                logger.info("按用户id[{}]严格查询用户完成", userIdStr);
            } else if (userName != null && !userName.isEmpty()) {
                // 按用户名模糊查询（原有逻辑）
                result = userInformationService.queryUserByName(userName);
                logger.info("按用户名[{}]模糊查询用户完成", userName);
            } else {
                // 查询所有用户（原有逻辑）
                ResultDTO<List<UserInformation>> allResult = userInformationService.queryAllUsers();
                result = allResult;
            }
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("查询数量格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("查询数量格式错误")));
        } catch (Exception e) {
            logger.error("查询用户信息异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("查询失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // ========== doPost 原有逻辑 【无修改】 ==========
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        UserInformation user = JSON.parseObject(req.getInputStream(), UserInformation.class);

        try {
            ResultDTO<Void> result = userInformationService.addUser(user);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("新增用户异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("新增失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // ========== doDelete 原有逻辑 【无修改】 ==========
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String userIdStr = req.getParameter("userId");

        try {
            if (userIdStr == null || userIdStr.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("用户ID不能为空")));
                out.flush();
                out.close();
                return;
            }
            ResultDTO<Void> result = userInformationService.deleteUser(userIdStr);
            out.write(JSON.toJSONString(result));
        } catch (NumberFormatException e) {
            logger.error("用户ID格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("用户ID必须是数字")));
        } catch (Exception e) {
            logger.error("删除用户异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("删除失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // ========== doPut 原有逻辑 【无修改】（会自动追加Emoji） ==========
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        UserInformation user = JSON.parseObject(req.getInputStream(), UserInformation.class);

        try {
            ResultDTO<Void> result = userInformationService.updateUser(user);
            out.write(JSON.toJSONString(result));
        } catch (Exception e) {
            logger.error("更新用户异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 处理更新请求（PUT）
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireLogin(req, resp)) return;
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        // 区分业务类型：normal=普通更新(阅读时间/资料)  token=更新登录令牌
        String operateType = req.getParameter("type");

        try {
            // ===================== 分支1：普通更新（阅读时间、个人资料等原有逻辑） =====================
            if ("normal".equals(operateType) || operateType == null || operateType.isEmpty()) {
                UserInformation user = JSON.parseObject(req.getInputStream(), UserInformation.class);
                ResultDTO<Void> result = userInformationService.updateUserStock(user);
                logger.info("普通Patch更新用户信息完成，用户ID:{}", user.getUserId());
                out.write(JSON.toJSONString(result));
            }
            // ===================== 分支2：更新登录Token（记住我功能专用） =====================
            else if ("token".equals(operateType)) {
                // 先把输入流读取为字符串
                byte[] buffer = req.getInputStream().readAllBytes();
                String jsonStr = new String(buffer, StandardCharsets.UTF_8);
                // 字符串 + TypeReference 解析Map
                Map<String, String> paramMap = JSON.parseObject(jsonStr, new TypeReference<Map<String, String>>() {});

                String userId = paramMap.get("userId");
                String token = paramMap.get("token");
                String expireTimeStr = paramMap.get("expireTime");

                // 非空校验
                if (userId == null || userId.isEmpty()) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("用户ID不能为空")));
                    return;
                }
                if (token == null || token.isEmpty()) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("登录Token不能为空")));
                    return;
                }
                if (expireTimeStr == null || expireTimeStr.isEmpty()) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("Token过期时间不能为空")));
                    return;
                }

                LocalDateTime expireTime = LocalDateTime.parse(expireTimeStr, DATE_TIME_FORMAT);
                ResultDTO<Void> result = userInformationService.updateUserToken(userId, token, expireTime);
                logger.info("Token-Patch更新登录凭证完成，用户ID:{}", userId);
                out.write(JSON.toJSONString(result));
            }
            // 🔥 新增：清空 Token 的分支
            else if ("clearToken".equals(operateType)) {
                byte[] buffer = req.getInputStream().readAllBytes();
                String jsonStr = new String(buffer, StandardCharsets.UTF_8);
                Map<String, String> paramMap = JSON.parseObject(jsonStr, new TypeReference<Map<String, String>>() {});

                String userId = paramMap.get("userId");
                if (userId == null || userId.isEmpty()) {
                    out.write(JSON.toJSONString(ResultDTO.paramError("用户ID不能为空")));
                    return;
                }

                // 把 token 和 过期时间 置为 null
                ResultDTO<Void> result = userInformationService.updateUserToken(userId, null, null);
                logger.info("退出登录，清空登录凭证完成，用户ID:{}", userId);
                out.write(JSON.toJSONString(result));
            }
            // ===================== 非法类型 =====================
            else {
                out.write(JSON.toJSONString(ResultDTO.paramError("不支持的操作类型")));
            }
        } catch (DateTimeParseException e) {
            logger.error("Token过期时间格式错误", e);
            out.write(JSON.toJSONString(ResultDTO.paramError("时间格式错误，请使用 yyyy-MM-dd HH:mm:ss")));
        } catch (Exception e) {
            logger.error("Patch请求处理异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("更新失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}
