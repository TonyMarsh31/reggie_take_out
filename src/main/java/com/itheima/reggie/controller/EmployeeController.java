package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * 员工登录
     */
    @PostMapping("/login")
    public R<Employee> login(@RequestBody Employee employee, HttpServletRequest request) {
        // 1.将页面提交的密码进行加密
        String password = DigestUtils.md5DigestAsHex(employee.getPassword().getBytes());
        // 2. 根据用户名查询数据库
        LambdaQueryWrapper<Employee> employeeLambdaQueryWrapper = new LambdaQueryWrapper<>();
        employeeLambdaQueryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee result = employeeService.getOne(employeeLambdaQueryWrapper);
        // 3. 如果没有查询则直接返回登录失败结果
        if (result == null) {
            return R.error("登陆失败，没有该用户名");
        }
        // 4. 如果查询到了，则进一步判断密码是否正确
        if (!password.equals(result.getPassword())) {
            return R.error("登陆失败，密码错误");
        }
        // 5. 查看员工状态是否被禁用
        if (result.getStatus() == 0) {
            return R.error("登陆失败，该用户已被禁用");
        }
        // 6. 登录成功，将用户Id信息存入session
        request.getSession().setAttribute("employee", result.getId());
        // 7. 将用户信息返回客户端
        return R.success(result);
    }

    /**
     * 员工退出登录
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request) {
        // 1. 清除session中的用户Id信息
        request.getSession().removeAttribute("employee");
        // 2. 返回成功结果
        return R.success("退出成功");
    }
}
