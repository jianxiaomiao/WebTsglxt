package com.example.servlet;

// 适配你的Jakarta EE项目
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 聚合数据 汉字字典API对接（完全对齐官方示例）
 */
@WebServlet("/api/dictionary")
public class DictionaryServlet extends HttpServlet {

    // ====================== 🔑 替换成你的API Key ======================
    private static final String API_KEY = "621f7e143649d644b4aa0eac2119ec50";
    // =================================================================

    // 缓存：节省免费额度，避免重复调用
    private static final Map<String, String> cache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE_TIME = 24 * 60 * 60 * 1000; // 缓存24小时
    private static final Map<String, Long> cacheTime = new ConcurrentHashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*"); // 跨域配置

        String word = req.getParameter("word");
        if (word == null || word.trim().isEmpty()) {
            resp.getWriter().write("{\"error_code\":1001,\"reason\":\"请输入要查询的汉字\",\"result\":null}");
            return;
        }

        // 1. 优先查本地缓存，避免重复调用API
        String cachedResult = getFromCache(word);
        if (cachedResult != null) {
            resp.getWriter().write(cachedResult);
            return;
        }

        // 2. 按照官方示例拼接参数（关键修复：规范参数编码+添加dtype）
        try {
            String apiUrl = "https://v.juhe.cn/xhzd/query";
            Map<String, String> paramsMap = new HashMap<>();
            paramsMap.put("key", API_KEY);
            paramsMap.put("word", word);
            paramsMap.put("dtype", "json"); // 强制返回JSON格式，和官方示例一致

            // 用官方params方法拼接URL（避免手动编码错误）
            String fullUrl = apiUrl + "?" + params(paramsMap);

            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // 读取API响应
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String result = response.toString();

            // 3. 存入缓存
            putToCache(word, result);

            // 4. 返回给前端
            resp.getWriter().write(result);

        } catch (Exception e) {
            e.printStackTrace();
            // 异常返回格式，保证前端能解析
            resp.getWriter().write("{\"error_code\":9999,\"reason\":\"API调用失败：" + e.getMessage() + "\",\"result\":null}");
        }
    }

    // 🔧 完全照搬官方示例的参数拼接方法（关键修复：统一编码所有参数）
    public static String params(Map<String, String> map) {
        return map.entrySet().stream()
                .map(entry -> {
                    try {
                        return entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return entry.getKey() + "=" + entry.getValue();
                    }
                })
                .collect(Collectors.joining("&"));
    }

    // 🔧 缓存工具方法（保留原有逻辑，节省免费额度）
    private String getFromCache(String word) {
        Long time = cacheTime.get(word);
        if (time != null && System.currentTimeMillis() - time < CACHE_EXPIRE_TIME) {
            return cache.get(word);
        }
        cache.remove(word);
        cacheTime.remove(word);
        return null;
    }

    private void putToCache(String word, String result) {
        cache.put(word, result);
        cacheTime.put(word, System.currentTimeMillis());
    }
}