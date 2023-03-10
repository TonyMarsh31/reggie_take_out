package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.mapper.EmployeeMapper;
import com.itheima.reggie.service.EmployeeService;
import org.springframework.stereotype.Service;

@Service
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    @Override
    public Page<Employee> getDataByNameAsPage(int page, int pageSize, String name) {
        Page<Employee> employeePage = new Page<>(page, pageSize);
        lambdaQuery().like(name != null, Employee::getName, name).page(employeePage);
        return employeePage;
    }
}
