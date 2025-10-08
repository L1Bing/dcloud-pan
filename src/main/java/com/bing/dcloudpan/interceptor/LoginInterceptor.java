package com.bing.dcloudpan.interceptor;

import com.bing.dcloudpan.dto.AccountDTO;
import com.bing.dcloudpan.enums.BizCodeEnum;
import com.bing.dcloudpan.util.CommonUtil;
import com.bing.dcloudpan.util.JsonData;
import com.bing.dcloudpan.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    public static ThreadLocal<AccountDTO> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 处理OPTIONS请求
        if (HttpMethod.OPTIONS.toString().equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return true;
        }

        // 从请求头或参数中获取token
        String token = request.getHeader("token");
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }

        // 如果token存在，解析JWT
        if (StringUtils.isNotBlank(token)) {
            Claims claims = JwtUtil.checkLoginJWT(token);
            if (claims == null) {
                // 如果token无效，返回未登录的错误信息
                CommonUtil.sendJsonMessage(response, JsonData.buildResult(BizCodeEnum.ACCOUNT_UNLOGIN));
                return false;
            }

            // 从JWT中提取用户信息
            Long accountId = Long.valueOf( claims.get("accountId")+"");
            String userName = (String) claims.get("username");
            // 创建 AccountDTO 对象
            AccountDTO accountDTO = AccountDTO.builder()
                    .id(accountId)
                    .username(userName)
                    .build();


            // 将用户信息存入 ThreadLocal
            threadLocal.set(accountDTO);
            return true;
        }

        // 如果没有token，返回未登录的错误信息
        CommonUtil.sendJsonMessage(response, JsonData.buildResult(BizCodeEnum.ACCOUNT_UNLOGIN));
        return false;
    }
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理 ThreadLocal 中的用户信息
        threadLocal.remove();
    }
}
