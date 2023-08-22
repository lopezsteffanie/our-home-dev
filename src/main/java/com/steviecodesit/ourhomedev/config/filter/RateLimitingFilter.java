package com.steviecodesit.ourhomedev.config.filter;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitingFilter implements Filter {

    private final RateLimiter rateLimiter = RateLimiter.create(1.0);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!rateLimiter.tryAcquire()) {
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setStatus(429); // 429 is the HTTP status code for "Too Many Requests"
            httpServletResponse.getWriter().write("Too many requests");
            return;
        }
        chain.doFilter(request, response);
    }
}
