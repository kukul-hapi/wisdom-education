package com.education.api.config.auth;

import com.education.auth.AuthConfig;
import com.education.auth.AuthHandler;
import com.education.auth.AuthRealmManager;
import com.education.auth.session.RedisSessionStorage;
import com.education.auth.token.JwtTokenFactory;
import com.education.common.cache.CacheBean;
import com.education.common.model.JwtToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * auth 认证配置
 * @author zengjintao
 * @create_at 2021年11月27日 0027 11:47
 * @since version 1.0.4
 */
@Configuration
public class AuthBeanConfig {

    @Resource
    private AdminLoginRealm adminLoginRealm;
    @Resource
    private StudentLoginRealm studentLoginRealm;

    @Bean
    public AuthRealmManager authRealmManager(JwtToken jwtToken,
                                             AuthConfig authConfig,
                                             CacheBean redisCacheBean) {
        AuthRealmManager authRealmManager = new AuthRealmManager(
                new RedisSessionStorage(redisCacheBean, authConfig.getSessionIdPrefix()),
                new JwtTokenFactory(jwtToken), authConfig);
        authRealmManager.addLoginAuthRealm(adminLoginRealm);
        authRealmManager.addLoginAuthRealm(studentLoginRealm);
        return authRealmManager;
    }

    @Bean
    public AuthHandler authHandler() {
        return new AuthHandler();
    }
}
