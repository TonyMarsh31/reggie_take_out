package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.exception.ObjectContainsNestedProperties;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final SetmealService setmealService;
    @Autowired
    private DishService dishService;

    public CategoryServiceImpl(SetmealService setmealService) {
        this.setmealService = setmealService;
    }

    /**
     * 根据id删除分类，删除前进行判断:如果分类下有菜品或套餐，不允许删除
     *
     * @param id 分类id
     */
    @Override
    public void remove(Long id) {
        //先查询该分类下是否有菜品或套餐
        LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
        dishQueryWrapper.eq(Dish::getCategoryId, id);
        int count = dishService.count(dishQueryWrapper);
        if (count > 0) {
            throw new ObjectContainsNestedProperties("该分类下已有菜品，无法进行删除");
        }
        LambdaQueryWrapper<Setmeal> setmealQueryWrapper = new LambdaQueryWrapper<>();
        setmealQueryWrapper.eq(Setmeal::getCategoryId, id);
        int count1 = setmealService.count(setmealQueryWrapper);
        if (count1 > 0) {
            throw new ObjectContainsNestedProperties("该分类下已有套餐，无法进行删除");
        }
        //如果没有，就删除
        baseMapper.deleteById(id);
    }
}
