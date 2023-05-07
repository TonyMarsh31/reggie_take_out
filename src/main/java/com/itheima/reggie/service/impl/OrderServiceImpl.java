package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrderMapper;
import com.itheima.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    private final ShoppingCartService shoppingCartService;
    private final UserService userService;
    private final AddressBookService addressBookService;
    private final OrderDetailService orderDetailService;

    public OrderServiceImpl(ShoppingCartService shoppingCartService, UserService userService, AddressBookService addressBookService, OrderDetailService orderDetailService) {
        this.shoppingCartService = shoppingCartService;
        this.userService = userService;
        this.addressBookService = addressBookService;
        this.orderDetailService = orderDetailService;
    }

    /**
     * 用户下单,即向订单表和订单明细表插入数据,完成后删除购物车中的数据
     */
    @Transactional
    public void submit(Orders orders) {
        //获得当前用户id
        Long userId = BaseContext.getCurrentId();
        //查询用户数据
        User user = userService.getById(userId);
        //查询地址数据
        AddressBook addressBook = addressBookService.getById(orders.getAddressBookId());

        // 异常情况
        if (addressBook == null) throw new RuntimeException("用户地址信息有误，不能下单");
        //查询当前用户的购物车数据
        List<ShoppingCart> shoppingCarts = shoppingCartService.lambdaQuery().eq(ShoppingCart::getUserId, userId).list();
        if (shoppingCarts == null || shoppingCarts.size() == 0) throw new RuntimeException("购物车为空，不能下单");


        //使用mybatis提供的工具类生成订单号
        long orderId = IdWorker.getId();
        // 计算订单总金额
        AtomicInteger amount = new AtomicInteger(0);
        //处理订单明细表: 订单明细表的数据来源于购物车表
        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            // 计算订单总金额: 订单总金额 = 订单明细表中所有的金额(单价 乘于 份数)之和
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());
        //向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetails);

        //处理订单表
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));//总金额
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" :
                addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        //向订单表插入数据，一条数据
        save(orders);


        //清空购物车数据
        shoppingCartService.remove(new LambdaQueryWrapper<ShoppingCart>().eq(ShoppingCart::getUserId, userId));
    }

    @Override
    public Page<Orders> getDataAsPage(int page, int pageSize, Long number, String beginTime, String endTime) {
        Page<Orders> ordersPage = new Page<>(page, pageSize);
        lambdaQuery()
                .eq(number != null, Orders::getId, number) //订单号
                //时间段，大于开始，小于结束
                .gt(StringUtils.hasText(beginTime), Orders::getOrderTime, beginTime)
                .lt(StringUtils.hasText(endTime), Orders::getOrderTime, endTime)
                .page(ordersPage);
        return ordersPage;
    }
}