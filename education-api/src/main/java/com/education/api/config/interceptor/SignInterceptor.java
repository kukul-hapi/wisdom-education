package com.education.api.config.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.AES;
import com.education.business.interceptor.BaseInterceptor;
import com.education.common.constants.AuthConstants;
import com.education.common.exception.BusinessException;
import com.education.common.utils.IpUtils;
import com.education.common.utils.ObjectUtils;
import com.education.common.utils.RequestUtils;
import com.education.common.utils.ResultCode;
import com.jfinal.kit.HashKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * http参数签名拦截器
 */
@Component
public class SignInterceptor extends BaseInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(SignInterceptor.class);
    private static final String SIGN = AuthConstants.SIGN;
    private static final String AES_KEY = "1964d18695d7f8ad";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        this.checkSign(request);
        return super.preHandle(request, response, handler);
    }


    private void checkSign(HttpServletRequest request) {
        String headerSign = request.getHeader(SIGN);
        String ip = IpUtils.getAddressIp(request);
        if (ObjectUtils.isEmpty(headerSign)) {
            logger.warn("【签名校验失败】客户端ip:{}请求头未携带:{}", ip, SIGN);
            throw new BusinessException(new ResultCode(ResultCode.UN_AUTH_HEADER, "请求头未携带:" + SIGN));
        }

        Map<String, String> params = request.getParameterMap()
                .keySet().stream()
                .collect(Collectors.toMap(Function.identity(),
                        k -> String.join(",", request.getParameterValues(k))));

        String json = readData(request);
        String paramSign;
        if (StrUtil.isNotBlank(json) && JSONUtil.isJson(json)) {
            paramSign = AES.encrypt(json, AES_KEY);
        } else {
            // 处理url 参数
            paramSign = AES.encrypt(JSONUtil.toJsonStr(params), AES_KEY);
        }
        if (!HashKit.md5(paramSign).equals(headerSign)) {
            String targetUrl = RequestUtils.getRequestUrl(request);
            logger.warn("【签名校验失败】目标ip:{}, 请求url:{}: 非法参数签名:{}, 正确签名值:{}",
                    ip, targetUrl, paramSign, headerSign);
            throw new BusinessException(new ResultCode(ResultCode.UN_AUTH_HEADER, "参数签名校验失败"));
        }
    }
}
