package com.itheima.reggie.controller;

import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 购物车
 */
@Slf4j
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    public ShoppingCartController(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    //TODO : /add 和 /sub 接口的设计不合理，需要完善购物车的添加与删除逻辑
    // 用户端不能直接选择购买数量，而是通过点击+号和-号来增加或减少购买数量，这样可以避免用户输入错误的购买数量
    // 但是这样每次点击+号或-号都要发送一个请求，这样会导致请求过多，影响性能
    // 可以通过优化前端代码来完成购物车数据的处理，或者使用Redis缓存购物车数据,总之要减少对数据库的写频率

    /**
     * 添加商品到购物车,或增加购买数量
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart reqData) {
        log.info("购物车数据:{}", reqData);
        //查询当前要操作的商品是否已经在用户的购物车中
        ShoppingCart cartDataInDB = shoppingCartService.getShoppingCartByID(reqData);
        Optional<ShoppingCart> cartDataOpt = Optional.ofNullable(cartDataInDB);
        cartDataOpt.ifPresent(dataInDB -> {
            // 如果已经存在,就设置购买数量+1，更新购物车
            dataInDB.setNumber(dataInDB.getNumber() + 1);
            shoppingCartService.updateById(dataInDB);
        });
        cartDataInDB = cartDataOpt.orElseGet(() -> {
            // 如果不存在,就正常执行添加操作,设置购买数量为1
            reqData.setNumber(1);
            reqData.setUserId(BaseContext.getCurrentId());
            reqData.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(reqData);
            return reqData;
        });
        return R.success(cartDataInDB);
    }

    /**
     * 从购物车中删除商品,或减少购买数量
     */
    @PostMapping("/sub")
    public R<ShoppingCart> sub(@RequestBody ShoppingCart reqData) {
        log.info("购物车数据:{}", reqData);
        ShoppingCart cartDataInDB = shoppingCartService.getShoppingCartByID(reqData);
        // 正常情况下,购物车中的商品数量不会小于1,这里只是为了防止用户恶意操作
        if (cartDataInDB.getNumber() > 1) {
            cartDataInDB.setNumber(cartDataInDB.getNumber() - 1);
            shoppingCartService.updateById(cartDataInDB);
        } else {
            shoppingCartService.removeById(cartDataInDB.getId());
            cartDataInDB.setNumber(0); // 这一步仅为符合前端的DTO规范：前端通过response.data.number来判断是否删除了商品
        }
        return R.success(cartDataInDB);
    }


    /**
     * 查看购物车
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        List<ShoppingCart> list = shoppingCartService
                .lambdaQuery()
                .eq(ShoppingCart::getUserId, BaseContext.getCurrentId())
                .orderByAsc(ShoppingCart::getCreateTime)
                .list();
        return R.success(list);
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/clean")
    public R<String> clean() {
        //SQL:delete from shopping_cart where user_id = ?
        shoppingCartService
                .lambdaUpdate()
                .eq(ShoppingCart::getUserId, BaseContext.getCurrentId())
                .remove();
        return R.success("清空购物车成功");
    }
}