package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.ShoppingCart;

public interface ShoppingCartService extends IService<ShoppingCart> {
    /**
     * 根据用户id和菜品id/套餐id查询购物车数据,
     */
    ShoppingCart getShoppingCartByID(ShoppingCart conditionWrapper);
}
