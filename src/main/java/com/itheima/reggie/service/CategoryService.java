package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {
    void remove(Long id);

    List<Category> getCategoryAsList(Category conditionWrapper);

    Page<Category> getCategoryAsPage(int page, int pageSize);

}
