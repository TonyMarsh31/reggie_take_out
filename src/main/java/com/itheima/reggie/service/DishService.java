package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     * 需要进行逻辑删除的信息有：菜品信息、菜品口味信息
     * 同时还需要判定，如果菜品目前正在起售中，那么不能删除
     * (由于只要菜品还在一个起售的套餐中，那么该菜品就无法停售，所以这里只要判断菜品是否起售即可，无需考虑与套餐的关系)
     */
    void deleteWithFlavor(List<Long> ids);

    /**
     * 修改菜品的销售状态：起售、停售
     *
     * @param status 销售状态 0：停售，1：起售
     * @param ids    菜品id集合
     */
    void updateStatus(Integer status, List<Long> ids);

    /**
     * 查询菜品的同时，将口味数据也查询出来
     *
     * @param id 菜品id
     * @return 菜品数据dto
     */
    DishDto getDishDtoWithFlavorById(Long id);

    /**
     * 查询菜品分页数据
     *
     * @param pageWrapper 分页参数包装类
     * @param queryName   菜品名称
     */
    void getDishPage(Page<Dish> pageWrapper, String queryName);

    /**
     * 将菜品分页数据转为dto分页数据，dto中增加了口味数据与分类名称
     *
     * @param dishPage 菜品分页数据
     * @return 菜品dto分页数据
     */
    Page<DishDto> convertDishPageToDishDtoPage(Page<Dish> dishPage);
}
