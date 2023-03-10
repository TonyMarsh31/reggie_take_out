package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Employee;

public interface EmployeeService extends IService<Employee> {
    Page<Employee> getDataByNameAsPage(int page, int pageSize, String name);
}
