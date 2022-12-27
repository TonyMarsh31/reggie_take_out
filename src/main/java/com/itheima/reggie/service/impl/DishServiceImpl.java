package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.exception.ObjectStillOnStockException;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    private final DishFlavorService dishFlavorService;
    private final SetmealService setmealService;
    private final SetmealDishService setmealDishService;

    public DishServiceImpl(DishFlavorService dishFlavorService, SetmealService setmealService, SetmealService setmealService1, SetmealDishService setmealDishService) {
        this.dishFlavorService = dishFlavorService;
        this.setmealService = setmealService1;
        this.setmealDishService = setmealDishService;
    }


    /**
     * 新增菜品的同时，将口味数据也新增到对应数据表中
     *
     * @param dishDto 前端传递的包含口味信息的菜品数据
     */
    @Transactional
    @Override
    public void saveWithFlavor(DishDto dishDto) {
        // 保存菜品基本信息
        this.save(dishDto);
        // 保存菜品口味信息
        // 前端传递的口味数据中只有name和value，这里手动为每一个flavor绑定dishID
        Long dishID = dishDto.getId();
        List<DishFlavor> flavors = dishDto.getFlavors();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishID);
        }
        dishFlavorService.saveBatch(flavors);
    }

    @Transactional
    @Override
    public void updateWithFlavor(DishDto dishDto) {
        // 修改菜品基本信息
        this.updateById(dishDto);
        // 前端传递的口味数据中只有name和value，这里手动为每一个flavor绑定dishID
        Long dishID = dishDto.getId();
        List<DishFlavor> flavors = dishDto.getFlavors();
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishID);
        }
        // 先删除原有的口味数据
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishID);
        dishFlavorService.remove(queryWrapper);
        // 再新增新的口味数据
        dishFlavorService.saveBatch(flavors);
    }

    @Override
    public DishDto getDishDtoWithFlavorById(Long id) {
        Dish dish = this.getById(id);
        // 查询口味信息
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, id);
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        // 将菜品基本信息和口味信息封装到dto中
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);
        dishDto.setFlavors(flavors);
        return dishDto;
    }

    /**
     * 修改菜品的销售状态：起售、停售
     *
     * @param status 销售状态, 0停售，1起售
     * @param ids    菜品id集合
     */
    @Override
    public void updateStatus(Integer status, List<Long> ids) {
        // 当菜品在一个起售中的套餐中时，无法进行停售操作，提示用户需要先停售套餐

        // 1. 查询套餐表中正在起售的套餐
        // 2. 根据起售的套餐id查询 套餐菜品关联表，得到所有的在起售套餐中的菜品id
        // 3. 判断：如果前端传递的菜品id中包含了在起售套餐中的菜品id，则说明这个菜品在起售中的套餐中，无法进行停售操作
        if (status == 0) {
            LambdaQueryWrapper<Setmeal> setmealOnSale = new LambdaQueryWrapper<>();
            setmealOnSale.eq(Setmeal::getStatus, 1);
            setmealService.list(setmealOnSale).stream().map(Setmeal::getId).forEach(setmealId -> {
                LambdaQueryWrapper<SetmealDish> setmealDishOnSale = new LambdaQueryWrapper<>();
                setmealDishOnSale.eq(SetmealDish::getSetmealId, setmealId);
                List<SetmealDish> onSaleSetmealDish = setmealDishService.list(setmealDishOnSale);
                onSaleSetmealDish.stream().map(SetmealDish::getDishId).forEach(onSaleDishID -> {
                    if (ids.contains(onSaleDishID)) {
                        throw new ObjectStillOnStockException("存在菜品在起售中的套餐中，无法进行停售操作");
                    }
                });
            });
        }

        //正常的停售与起售菜品操作
        Dish newStatus = new Dish();
        newStatus.setStatus(status);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Dish::getId, ids);
        this.update(newStatus, queryWrapper);
    }
}
