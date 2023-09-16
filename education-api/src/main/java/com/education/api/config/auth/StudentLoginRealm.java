package com.education.api.config.auth;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.education.auth.AuthConfig;
import com.education.auth.realm.LoginAuthRealm;
import com.education.auth.LoginToken;
import com.education.business.service.education.GradeInfoService;
import com.education.business.service.education.StudentInfoService;
import com.education.business.session.StudentSession;
import com.education.business.task.TaskManager;
import com.education.business.task.param.WebSocketMessageParam;
import com.education.common.constants.CacheTime;
import com.education.common.constants.LocalQueueConstants;
import com.education.common.enums.LoginEnum;
import com.education.common.enums.SocketMessageTypeEnum;
import com.education.common.exception.BusinessException;
import com.education.common.utils.IpUtils;
import com.education.common.utils.ObjectUtils;
import com.education.common.utils.PasswordUtil;
import com.education.common.utils.RequestUtils;
import com.education.model.entity.GradeInfo;
import com.education.model.entity.StudentInfo;
import com.jfinal.kit.HashKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/**
 * @author zengjintao
 * @create_at 2021年11月30日 0030 14:28
 * @since version 1.0.4
 */
@Component
public class StudentLoginRealm implements LoginAuthRealm<StudentSession> {

    private final Logger logger = LoggerFactory.getLogger(StudentLoginRealm.class);
    @Resource
    private StudentInfoService studentInfoService;
    @Resource
    private GradeInfoService gradeInfoService;
    @Resource
    private TaskManager taskManager;
    @Resource
    private AuthConfig authConfig;

    @Override
    public StudentSession doLogin(LoginToken loginToken) {
        LambdaQueryWrapper queryWrapper = Wrappers.<StudentInfo>lambdaQuery()
                .eq(StudentInfo::getLoginName, loginToken.getUsername());
        StudentInfo studentInfo = studentInfoService.selectFirst(queryWrapper);
        if (ObjectUtils.isEmpty(studentInfo)) {
            throw new BusinessException("用户不存在");
        }

        String dataBasePassword = studentInfo.getPassword();
        String encrypt = studentInfo.getEncrypt();
        if (!dataBasePassword.equals(PasswordUtil.createPassword(encrypt, loginToken.getPassword()))) {
            throw new BusinessException("用户名或密码错误");
        }

        if (studentInfo.isDisabledFlag()) {
            throw new BusinessException("账号已被禁用");
        }

        GradeInfo gradeInfo = gradeInfoService.getById(studentInfo.getGradeInfoId());
        StudentSession studentSession = new StudentSession(studentInfo.getId());
        studentSession.setName(studentInfo.getName());
        studentSession.setLoginName(studentInfo.getLoginName());
        studentSession.setHeadImg(studentInfo.getHeadImg());
        studentSession.setSex(studentInfo.getSex());
        studentSession.setAge(studentInfo.getAge());
        studentSession.setBirthday(studentInfo.getBirthday());
        studentSession.setAddress(studentInfo.getAddress());
        studentSession.setSex(studentInfo.getSex());
        studentSession.setMobile(studentInfo.getMobile());
        studentSession.setLoginCount(studentInfo.getLoginCount());
        studentSession.setGradeInfoId(gradeInfo.getId());
        studentSession.setGradeInfoName(gradeInfo.getName());
        return studentSession;
    }

    @Override
    public String getLoginType() {
        return LoginEnum.STUDENT.getValue();
    }

    @Override
    public void onLoginSuccess(StudentSession userSession) {
        studentInfoService.updateLoginInfo(userSession.getId(), userSession.getLoginCount());
    }

    @Override
    public void onLogoutSuccess(StudentSession userSession) {

    }

    @Override
    public void onRejectSession(StudentSession userSession) {
        String hashToken = HashKit.md5(userSession.getToken());
        logger.warn("学员:{}会话token:{}被挤下线", userSession.getLoginName(),
                authConfig.getSessionIdPrefix() + StrUtil.COLON + LoginEnum.STUDENT.getValue() +
                        StrUtil.COLON + hashToken);
        WebSocketMessageParam taskParam = new WebSocketMessageParam(LocalQueueConstants.SYSTEM_SOCKET_MESSAGE);
        taskParam.setHashToken(HashKit.md5(userSession.getToken()));
        taskParam.setSocketMessageTypeEnum(SocketMessageTypeEnum.REJECT_SESSION);
        taskParam.setIp(IpUtils.getAddressIp(RequestUtils.getRequest()));
        taskManager.pushTask(taskParam);
    }

    @Override
    public long getSessionTimeOut(boolean rememberMe) {
        if (rememberMe) {
            // 缓存一周
            return CacheTime.ONE_WEEK_SECOND;
        }
        // 考虑到有点试卷可能考试时间在2个小时，默认将token失效时间先设置为3个小时
        return CacheTime.THREE_HOUR_SECOND;
    }

    @Override
    public void onLoginFail(String username, Exception e) {

    }
}
