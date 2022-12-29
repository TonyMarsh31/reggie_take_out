package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.exception.ObjectStillOnStockException;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    private final SetmealDishService setmealDishService;

    public SetmealServiceImpl(SetmealDishService setmealDishService) {
        this.setmealDishService = setmealDishService;
    }

    /**
     * 保存套餐的基本信息，以及该套餐对应的菜品信息
     *
     * @param setmealDto 套餐信息
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        // 1. 保存套餐的基本信息
        this.save(setmealDto);
        // 2. 保存套餐对应的菜品信息到关系表中
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        // DTO中的菜品数据缺少套餐id，需要手动设置
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealDto.getId()));
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 更新套餐的基本信息，以及该套餐对应的菜品信息
     *
     * @param setmealDto 套餐信息dto
     */
    @Override
    public void updateWithDish(SetmealDto setmealDto) {
        // 1. 更新套餐的基本信息
        this.updateById(setmealDto);
        // 2. 更新套餐对应的菜品信息到关系表中,先删除，再新增
        setmealDishService.remove(new LambdaQueryWrapper<SetmealDish>()
                .eq(SetmealDish::getSetmealId, setmealDto.getId()));
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        // DTO中的菜品数据缺少套餐id，需要手动设置
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealDto.getId()));
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 删除套餐的基本信息，以及该套餐对应的菜品信息
     *
     * @param ids 套餐主键列表
     */
    @Override
    public void deleteWithDish(List<Long> ids) {
        // 先判断是否仍在售卖中，是则取消删除
        LambdaQueryWrapper<Setmeal> objectOnStock = new LambdaQueryWrapper<>();
        objectOnStock.in(Setmeal::getId, ids).eq(Setmeal::getStatus, 1);
        if (this.count(objectOnStock) > 0) {
            throw new ObjectStillOnStockException();
        }
        // 需要删除的： 1. 套餐基本信息 2. 套餐对应的菜品信息 , 全部进行逻辑删除
        this.update(new Setmeal().setIsDeleted(1), new LambdaQueryWrapper<Setmeal>().in(Setmeal::getId, ids));
        setmealDishService.update(new SetmealDish().setIsDeleted(1), new LambdaQueryWrapper<SetmealDish>().in(SetmealDish::getSetmealId, ids));
        // 也可以使用LambdaUpdateWrapper
        // setmealDishService.update(new UpdateWrapper<SetmealDish>().in("setmeal_id", ids).set("is_deleted", 1));
    }

    /**
     * 修改 停售、起售状态
     *
     * @param status 新的状态，1：起售，0: 停售
     * @param ids    套餐主键列表
     */
    @Override
    public void updateStatus(Integer status, List<Long> ids) {
        Setmeal newStatus = new Setmeal();
        newStatus.setStatus(status);
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);
        this.update(newStatus, queryWrapper);
    }
}
