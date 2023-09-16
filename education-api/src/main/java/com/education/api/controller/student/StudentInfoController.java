package com.education.api.controller.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.education.auth.AuthUtil;
import com.education.auth.LoginToken;
import com.education.business.service.education.StudentInfoService;
import com.education.business.service.education.SubjectInfoService;
import com.education.business.session.StudentSession;
import com.education.business.session.UserSessionContext;
import com.education.common.base.BaseController;
import com.education.common.constants.AuthConstants;
import com.education.common.enums.LoginEnum;
import com.education.common.utils.Result;
import com.education.common.utils.ResultCode;
import com.education.model.dto.StudentInfoDto;
import com.education.model.entity.StudentInfo;
import com.education.model.entity.SubjectInfo;
import com.education.model.request.UpdatePassword;
import com.education.model.request.UserLoginRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * 学员登录接口
 * @author zengjintao
 * @version 1.0
 * @create_at 2020/11/21 20:58
 */
@RestController("student-controller")
public class StudentInfoController extends BaseController {

    @Resource
    private StudentInfoService studentInfoService;
    @Resource
    private SubjectInfoService subjectInfoService;

    /**
     * 学员登录接口
     * @param userLoginRequest
     * @return
     */
    @PostMapping("login")
    public Result login(@RequestBody @Validated(UserLoginRequest.StudentLogin.class) UserLoginRequest userLoginRequest, HttpServletResponse response) {
        LoginToken loginToken = new LoginToken(userLoginRequest.getUserName(), userLoginRequest.getPassword(), LoginEnum.STUDENT.getValue(),
                userLoginRequest.isChecked(), userLoginRequest.getDeviceType());
        StudentSession session = AuthUtil.login(loginToken);
        response.addHeader(AuthConstants.AUTHORIZATION, session.getToken());
        return Result.success(ResultCode.SUCCESS, "登录成功", session);
    }

    @PostMapping("logout")
    public Result logout() {
        AuthUtil.logout(LoginEnum.STUDENT.getValue());
        return Result.success(ResultCode.SUCCESS, "退出成功");
    }

    /**
     * 修改新密码
     * @param updatePassword
     * @return
     */
    @PostMapping("/resetting/password")
    public Result updatePassword(@RequestBody @Validated UpdatePassword updatePassword) {
        return Result.success(studentInfoService.updatePassword(updatePassword.getPassword(),
                updatePassword.getNewPassword(), updatePassword.getConfirmPassword()));
    }


    /**
     * 获取当前学员学习科目
     * @return
     */
    @GetMapping("/subject")
    public Result getSubjectList() {
        LambdaQueryWrapper queryWrapper = Wrappers.<SubjectInfo>lambdaQuery()
                .eq(SubjectInfo::getGradeInfoId, UserSessionContext.getStudentUserSession().getGradeInfoId());
        return Result.success(subjectInfoService.list(queryWrapper));
    }

    /**
     * 修改个人资料
     * @param studentInfo
     * @return
     */
    @PostMapping("/info")
    public Result updateInfo(@RequestBody @Validated StudentInfo studentInfo) {
        return Result.success(studentInfoService.updateInfo(studentInfo));
    }
}
