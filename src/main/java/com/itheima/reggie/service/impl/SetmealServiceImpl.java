package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    private final SetmealDishService setmealDishService;

    public SetmealServiceImpl(SetmealDishService setmealDishService) {
        this.setmealDishService = setmealDishService;
    }

    /**
     * 保存套餐的基本信息，以及该套餐对应的菜品信息
     *
     * @param setmealDto 套餐信息
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        // 1. 保存套餐的基本信息
        this.save(setmealDto);
        // 2. 保存套餐对应的菜品信息到关系表中
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        // DTO中的菜品数据缺少套餐id，需要手动设置
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealDto.getId()));
        setmealDishService.saveBatch(setmealDishes);
    }
}
