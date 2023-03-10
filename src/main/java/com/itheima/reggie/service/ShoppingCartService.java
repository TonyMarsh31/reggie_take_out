package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService extends IService<ShoppingCart> {
    /**
     * 根据用户id和菜品id/套餐id查询购物车数据,
     */
    ShoppingCart getShoppingCartByID(ShoppingCart conditionWrapper);

    List<ShoppingCart> getDataByUserIDAsList(Long userId);

    void removeDataByUserID(Long userId);
}
