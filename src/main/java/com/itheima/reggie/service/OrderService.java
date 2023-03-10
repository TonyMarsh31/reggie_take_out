package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Orders;

public interface OrderService extends IService<Orders> {

    /**
     * 用户下单
     *
     * @param orders 订单信息
     */
    void submit(Orders orders);

    Page<Orders> getDataAsPage(int page, int pageSize, Long number, String beginTime, String endTime);
}
