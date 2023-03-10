package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

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
        Employee dataInDB = employeeService
                .lambdaQuery()
                .eq(Employee::getUsername, employee.getUsername())
                .one();
        // 3. 如果没有查询则直接返回登录失败结果
        if (dataInDB == null) return R.error("登陆失败，没有该用户名");
        // 4. 如果查询到了，则进一步判断密码是否正确
        if (!password.equals(dataInDB.getPassword())) return R.error("登陆失败，密码错误");
        // 5. 查看员工状态是否被禁用
        if (dataInDB.getStatus() == 0) return R.error("登陆失败，该用户已被禁用");
        // 6. 登录成功，将用户Id信息存入session
        request.getSession().setAttribute("employee", dataInDB.getId());
        // 7. 将用户信息返回客户端
        return R.success(dataInDB);
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

    /**
     * 新增员工
     *
     * @param employee 员工信息
     */
    @PostMapping
    public R<String> addNewEmployee(@RequestBody Employee employee) {
        // 1. 新员工默认密码为123456
        String password = DigestUtils.md5DigestAsHex("123456".getBytes());
        employee.setPassword(password);
        // 2. 保存员工信息
        employeeService.save(employee);
        // 3. 返回成功结果
        return R.success("新增成功");
    }

    /**
     * 员工信息的分页查询
     *
     * @param page     当前页
     * @param pageSize 每页显示的条数
     * @param name     查询条件
     */
    @GetMapping("/page")
    public R<Page<Employee>> page(int page, int pageSize, String name) {
        log.info("page:{},pageSize:{},name:{}", page, pageSize, name);
        Page<Employee> data = employeeService.getDataByNameAsPage(page, pageSize, name);
        return R.success(data);
    }

    /**
     * 根据员工Id修改员工信息
     *
     * @param employee 员工信息封装类，包含员工ID，与更新后的信息
     */
    @PutMapping
    public R<String> update(@RequestBody Employee employee) {
        employeeService.updateById(employee);
        return R.success("更新成功");
    }

    /**
     * 根据员工Id查询员工信息
     *
     * @param id 员工Id
     */
    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        return employee == null ? R.error("没有查询到该员工信息") : R.success(employee);
    }
}
