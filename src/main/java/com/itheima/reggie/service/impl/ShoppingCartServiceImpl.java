package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.mapper.ShoppingCartMapper;
import com.itheima.reggie.service.ShoppingCartService;
import org.springframework.stereotype.Service;

@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

    /**
     * 根据用户id和菜品id/套餐id查询购物车数据
     *
     * @param conditionWrapper 封装的条件
     */
    @Override
    public ShoppingCart getShoppingCartByID(ShoppingCart conditionWrapper) {
        return this.lambdaQuery()
                .eq(ShoppingCart::getUserId, BaseContext.getCurrentId())
                // 以下两个条件只会有一个成立,即查询的是菜品还是套餐
                .eq(conditionWrapper.getDishId() != null, ShoppingCart::getDishId, conditionWrapper.getDishId())
                .eq(conditionWrapper.getSetmealId() != null, ShoppingCart::getSetmealId, conditionWrapper.getSetmealId())
                .one();
    }
}
