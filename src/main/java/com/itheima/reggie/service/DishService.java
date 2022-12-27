package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    /**
     * 新增菜品的同时，将口味数据也新增到对应数据表中
     */
    void saveWithFlavor(DishDto dishDto);

    /**
     * 修改菜品的同时，将口味数据也进行更新
     */
    void updateWithFlavor(DishDto dishDto);

    /**
     * 查询菜品的同时，将口味数据也查询出来
     *
     * @param id 菜品id
     * @return 菜品数据dto
     */
    DishDto getDishDtoWithFlavorById(Long id);

    /**
     * 修改菜品的销售状态：起售、停售
     *
     * @param status 销售状态
     * @param ids    菜品id集合
     */
    void updateStatus(Integer status, List<Long> ids);
}
