package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 保存套餐的基本信息，以及该套餐对应的菜品信息
     *
     * @param setmealDto 套餐信息
     */
    void saveWithDish(SetmealDto setmealDto);
}
