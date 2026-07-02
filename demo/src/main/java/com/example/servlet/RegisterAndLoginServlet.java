package com.example.servlet;

import com.alibaba.fastjson.JSON;
import com.example.dao.impl.UserInformationDaoImpl;
import com.example.dto.LoginDTO;
import com.example.dto.RegisterDTO;
import com.example.dto.ResultDTO;
import com.example.dto.UserInfoDTO;
import com.example.entity.UserInformation;
import com.example.service.UserInformationService;
import com.example.service.impl.UserInformationServiceImpl;
import com.example.util.PasswordEncryptUtil;
import com.example.util.UserBehaviorLogger;
import com.example.util.LoginRateLimiter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

@WebServlet("/api/user/auth")
public class RegisterAndLoginServlet extends BaseServlet {
    private UserInformationDaoImpl userInformationDao;
    private UserInformationService userInformationService;
    private static final Logger logger = LoggerFactory.getLogger(RegisterAndLoginServlet.class);


    @Override
    public void init() throws ServletException {
        userInformationDao = new UserInformationDaoImpl();
        userInformationService = new UserInformationServiceImpl(userInformationDao);
    }


    // 处理注册请求（POST）
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeader(req, resp);
        resp.setContentType("application/json;charset=UTF-8");

        // RegisterAndLoginServlet.handleRegister 里添加
        logger.info("注册校验 SessionID {} " , req.getSession().getId());

        // 🔥 获取 URL 上的 action 参数
        String action = req.getParameter("action");

        if ("login".equals(action)) {
            handleLogin(req, resp);
        } else {
            // 默认或 action=register 走注册逻辑
            handleRegister(req, resp);
        }
    }

    // 🔥 抽取出来的【登录逻辑】
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        try {
            // 1. 解析前端传来的 JSON 请求体
            LoginDTO loginDTO = JSON.parseObject(req.getInputStream(), LoginDTO.class);

            // 2. 参数非空校验
            if (loginDTO.getUserid() == null || loginDTO.getUserid().isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("用户ID不能为空")));
                return;
            }

            // ==================== 🔒 登录频率限制 ====================
            String rateLimitKey = loginDTO.getUserid();
            if (LoginRateLimiter.isLocked(rateLimitKey)) {
                long remaining = LoginRateLimiter.getRemainingLockSeconds(rateLimitKey);
                long minutes = remaining / 60;
                long seconds = remaining % 60;
                out.write(JSON.toJSONString(ResultDTO.fail(
                    "登录尝试过于频繁，请在 " + minutes + "分" + seconds + "秒后再试")));
                return;
            }

            // 3. 查询用户
            ResultDTO<List<UserInformation>> userResult = userInformationService.queryUserById(loginDTO.getUserid());
            if (userResult.getCode() != 200 || userResult.getData() == null || userResult.getData().isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.fail("登录失败：用户不存在")));
                return;
            }
            UserInformation user = userResult.getData().get(0);

            // 4. 校验密码
            String encryptedPwd = PasswordEncryptUtil.encryptPassword(loginDTO.getPassword(), user.getSalt());
            if (!encryptedPwd.equals(user.getPassword())) {
                boolean locked = LoginRateLimiter.recordFailureAndCheckLocked(rateLimitKey);
                String msg = "登录失败：密码错误";
                if (locked) msg += "，已连续失败" + LoginRateLimiter.MAX_FAILURES + "次，账号锁定15分钟";
                out.write(JSON.toJSONString(ResultDTO.fail(msg)));
                return;
            }

            // 4.5 🔥 检查账号是否被管理员冻结
            if (user.getCan_use() != null && user.getCan_use() == 0) {
                out.write(JSON.toJSONString(ResultDTO.fail("账号已被冻结，请联系管理员")));
                return;
            }

            // 5. 登录成功：清除失败记录、处理记住我逻辑
            LoginRateLimiter.clearOnSuccess(rateLimitKey);
            HttpSession session = req.getSession();
            session.setAttribute("currentUser", user);

            String token = null;
            // 这里的 getRememberMe 是你在 LoginDTO 里的 Boolean
            if (Boolean.TRUE.equals(loginDTO.getRememberMe())) {
                token = java.util.UUID.randomUUID().toString().replace("-", "");
                java.time.LocalDateTime expireTime = java.time.LocalDateTime.now().plusDays(7);
                userInformationDao.updateToken(user.getUserId(), token, expireTime);
            }

            // 6. 返回数据
            UserInfoDTO userDTO = new UserInfoDTO(user);
            java.util.Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("user", userDTO);
            responseData.put("token", token);

            out.write(JSON.toJSONString(ResultDTO.success(responseData)));
            UserBehaviorLogger.logAsync(user.getUserId(), 28, null, null, "登录");
        } catch (Exception e) {
            logger.error("登录请求处理异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("登录失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }

    // 🔥 抽取出来的【注册逻辑】(把你原来 doPost 里的代码全搬进这个方法)
    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        try {
            RegisterDTO registerDTO = JSON.parseObject(req.getInputStream(), RegisterDTO.class);
            // 2. 参数校验
            // 2.1 非空校验
            if (registerDTO.getUserid() == null || registerDTO.getUserid().isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("用户ID不能为空")));
                return;
            }
            if (registerDTO.getUsername() == null || registerDTO.getUsername().isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("用户名不能为空")));
                return;
            }
            if (registerDTO.getPassword() == null || registerDTO.getPassword().isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("密码不能为空")));
                return;
            }
            if (registerDTO.getConfirmPassword() == null || registerDTO.getConfirmPassword().isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("确认密码不能为空")));
                return;
            }
            if (registerDTO.getSex() == null || registerDTO.getSex().isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("性别不能为空")));
                return;
            }
            // 2.2 密码一致性校验
            if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
                out.write(JSON.toJSONString(ResultDTO.paramError("确认密码与密码不一致")));
                return;
            }

            // 2.3 生日校验（加trim()去空格）
            if (registerDTO.getBirthday() != null && !registerDTO.getBirthday().trim().isEmpty()) {
                String birthdayStr = registerDTO.getBirthday().trim(); // 先把前端传的字符串存下来
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            .withResolverStyle(ResolverStyle.SMART);
                    LocalDate birthday = LocalDate.parse(birthdayStr, formatter);

                    // 生日超当前时间的提示（带转化后的日期）
                    if (birthday.isAfter(LocalDate.now())) {
                        String tipMsg = String.format("生日不能超过当前时间，您传入的日期为：%s", birthday);
                        out.write(JSON.toJSONString(ResultDTO.paramError(tipMsg)));
                        return;
                    }
                } catch (Exception e) {
                    // 格式错误的提示（带前端实际传入的字符串）
                    String errorMsg = String.format("生日格式错误（需为yyyy-MM-dd），您传入的日期为：%s", birthdayStr);
                    out.write(JSON.toJSONString(ResultDTO.paramError(errorMsg)));
                    return;
                }
            }

            // 校验邮箱验证码
            String email = registerDTO.getUserid(); // 现在 userid 就是邮箱了
            String inputCode = registerDTO.getEmailCode(); // 前端传来的验证码

            if (inputCode == null || inputCode.isEmpty()) {
                out.write(JSON.toJSONString(ResultDTO.paramError("邮箱验证码不能为空")));
                return;
            }

            HttpSession session = req.getSession();
            // 校验验证码（RegisterAndLoginServlet 中替换）
            String realCode = (String) session.getAttribute("EMAIL_CODE_" + email);
            Long sendTime = (Long) session.getAttribute("EMAIL_CODE_TIME_" + email);

            if (realCode == null || sendTime == null) {
                out.write(JSON.toJSONString(ResultDTO.paramError("验证码已过期或未发送")));
                return;
            }
            // 自行判断 5 分钟有效期
            if (System.currentTimeMillis() - sendTime > 5 * 60 * 1000) {
                session.removeAttribute("EMAIL_CODE_" + email);
                session.removeAttribute("EMAIL_CODE_TIME_" + email);
                out.write(JSON.toJSONString(ResultDTO.paramError("验证码已过期")));
                return;
            }

            // 验证通过后，马上清空Session里的验证码，防止重复使用
            session.removeAttribute("EMAIL_CODE_" + email);
            // 3. 密码加密
            String salt = PasswordEncryptUtil.generateSalt();
            String encryptedPwd = PasswordEncryptUtil.encryptPassword(registerDTO.getPassword(), salt);

            // 4. DTO转实体类
            UserInformation user = registerDTO.toUserInfo(encryptedPwd, salt);

            // 5. 调用Service新增用户
            ResultDTO<Void> result = userInformationService.addUser(user);
            logger.info("用户[{}]注册请求处理完成，结果：{},用户类型: {}", registerDTO.getUserid(), result.getMsg(),registerDTO.getUserType());
            out.write(JSON.toJSONString(result));
            UserBehaviorLogger.logAsync(user.getUserId(), 29, null, null, "注册");
        } catch (Exception e) {
            logger.error("注册请求处理异常", e);
            out.write(JSON.toJSONString(ResultDTO.fail("注册失败：" + e.getMessage())));
        } finally {
            out.flush();
            out.close();
        }
    }
}