package com.education.api.config.interceptor;

import cn.hutool.core.util.StrUtil;
import com.education.business.interceptor.BaseInterceptor;
import com.education.common.enums.LoginEnum;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 学员端api 拦截器
 * @author zengjintao
 * @version 1.0
 * @create_at 2020/11/21 21:38
 */
@Component
public class StudentAuthInterceptor extends BaseInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        // OPTIONS 请求直接放行，解决uni-app 请求跨域问题
        if (StrUtil.isNotBlank(method) && "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        super.checkHeader(request);
        return checkToken(LoginEnum.STUDENT.getValue(), response);
    }
}
