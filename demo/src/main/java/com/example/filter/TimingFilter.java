package com.example.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 请求耗时监控过滤器 — 超过 500ms 打 warn 日志
 */
@WebFilter("/*")
public class TimingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(TimingFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(req, resp);
        } finally {
            long ms = System.currentTimeMillis() - start;
            if (ms > 500) {
                HttpServletRequest httpReq = (HttpServletRequest) req;
                logger.warn("🐢 慢请求: {} {} → {}ms",
                        httpReq.getMethod(), httpReq.getRequestURI(), ms);
            }
        }
    }
}
