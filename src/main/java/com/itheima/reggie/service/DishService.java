package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;

public interface DishService extends IService<Dish> {

    /**
     * 新增菜品的同时，将口味数据也新增到对应数据表中
     */
    void saveWithFlavor(DishDto dishDto);

    /**
     * 查询菜品的同时，将口味数据也查询出来
     *
     * @param id 菜品id
     * @return 菜品数据dto
     */
    DishDto getDishDtoWithFlavorById(Long id);

}
