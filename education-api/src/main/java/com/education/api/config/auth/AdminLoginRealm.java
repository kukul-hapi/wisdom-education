package com.education.api.config.auth;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.education.auth.AuthConfig;
import com.education.auth.realm.LoginAuthRealm;
import com.education.auth.LoginToken;
import com.education.business.service.system.SystemAdminService;
import com.education.business.session.AdminUserSession;
import com.education.business.task.TaskManager;
import com.education.business.task.param.WebSocketMessageParam;
import com.education.common.constants.LocalQueueConstants;
import com.education.common.enums.BooleanEnum;
import com.education.common.enums.LoginEnum;
import com.education.common.enums.SocketMessageTypeEnum;
import com.education.common.exception.BusinessException;
import com.education.common.utils.IpUtils;
import com.education.common.utils.PasswordUtil;
import com.education.common.utils.RequestUtils;
import com.education.model.entity.SystemAdmin;
import com.jfinal.kit.HashKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;


/**
 * 管理员登录认证
 * @author zengjintao
 * @create_at 2021年11月27日 0027 11:28
 * @since version 1.0.4
 */
@Component
public class AdminLoginRealm implements LoginAuthRealm<AdminUserSession> {

    private final Logger logger = LoggerFactory.getLogger(AdminLoginRealm.class);
    @Resource
    private SystemAdminService systemAdminService;
    @Resource
    private TaskManager taskManager;
    @Resource
    private AuthConfig authConfig;

    @Override
    public AdminUserSession doLogin(LoginToken loginToken) {
        String password = loginToken.getPassword();
        LambdaQueryWrapper queryWrapper = Wrappers.<SystemAdmin>lambdaQuery()
                .eq(SystemAdmin::getLoginName, loginToken.getUsername());
        SystemAdmin systemAdmin = systemAdminService.getOne(queryWrapper);
        Assert.notNull(systemAdmin, () -> new BusinessException("用户名不存在"));
        password = PasswordUtil.createPassword(systemAdmin.getEncrypt(), password);
        if (!password.equals(systemAdmin.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        Integer flag = systemAdmin.getDisabledFlag();
        if (BooleanEnum.YES.getCode().equals(flag)) {
            throw new BusinessException("账号已被禁用");
        }
        AdminUserSession adminUserSession = new AdminUserSession(systemAdmin.getId());
        adminUserSession.setSystemAdmin(systemAdmin);
        adminUserSession.setLoginCount(systemAdmin.getLoginCount());
        return adminUserSession;
    }

    @Override
    public void loadPermission(AdminUserSession userSession) {
        systemAdminService.loadUserMenuAndPermission(userSession);
    }

    @Override
    public void onRejectSession(AdminUserSession userSession) {
        String hashToken = HashKit.md5(userSession.getToken());
        logger.warn("用户:{}会话token:{}被挤下线", userSession.getSystemAdmin().getLoginName(),
                authConfig.getSessionIdPrefix() + StrUtil.COLON + LoginEnum.ADMIN.getValue() +
                         StrUtil.COLON + hashToken);
        WebSocketMessageParam taskParam = new WebSocketMessageParam(LocalQueueConstants.SYSTEM_SOCKET_MESSAGE);
        taskParam.setHashToken(hashToken);
        taskParam.setSocketMessageTypeEnum(SocketMessageTypeEnum.REJECT_SESSION);
        taskParam.setIp(IpUtils.getAddressIp(RequestUtils.getRequest()));
        taskManager.pushTask(taskParam);
    }

    @Override
    public String getLoginType() {
        return LoginEnum.ADMIN.getValue();
    }
}
