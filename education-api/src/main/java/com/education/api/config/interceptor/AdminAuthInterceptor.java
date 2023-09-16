/**
 * 
 */
package com.education.api.config.interceptor;
import com.education.business.interceptor.BaseInterceptor;
import com.education.common.enums.LoginEnum;
import com.education.common.utils.RequestUtils;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户认证拦截器
 * @author zengjintao
 * @version 1.0
 * @create_at 2017年5月18日 下午2:41:05
 */
@Component
public class AdminAuthInterceptor extends BaseInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object object)
			throws Exception {
		super.checkHeader(request);
		String targetUrl = RequestUtils.getRequestUrl(request);
		if (targetUrl.startsWith("/api/dict")) {
			return true;
		}
		else if (targetUrl.startsWith("/api/upload")) {
			return true;
		}
		else if (targetUrl.startsWith("/uploads")) {
			return true;
		}
		return checkToken(LoginEnum.ADMIN.getValue(), response);
	}
}
