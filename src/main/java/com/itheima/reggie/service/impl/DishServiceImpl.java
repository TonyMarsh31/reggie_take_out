package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.exception.ObjectStillOnStockException;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    private final DishFlavorService dishFlavorService;
    private final SetmealService setmealService;
    private final SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    public DishServiceImpl(DishFlavorService dishFlavorService, SetmealService setmealService, SetmealDishService setmealDishService) {
        this.dishFlavorService = dishFlavorService;
        this.setmealService = setmealService;
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
        dishFlavorService
                .lambdaUpdate()
                .eq(DishFlavor::getDishId, dishID)
                .remove();
        // 再新增新的口味数据
        dishFlavorService.saveBatch(flavors);
    }

    @Override
    public DishDto getDishDtoWithFlavorById(Long id) {
        Dish dish = this.getById(id);
        // 查询口味信息
        List<DishFlavor> flavors = dishFlavorService
                .lambdaQuery()
                .eq(DishFlavor::getDishId, id)
                .list();
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
                List<SetmealDish> onSaleSetmealDish = setmealDishService
                        .lambdaQuery()
                        .eq(SetmealDish::getSetmealId, setmealId)
                        .list();
                onSaleSetmealDish.stream().map(SetmealDish::getDishId).forEach(onSaleDishID -> {
                    if (ids.contains(onSaleDishID)) {
                        throw new ObjectStillOnStockException("所要停售的部分菜品目前还在其他套餐中进行贩卖中，无法停售，请考虑先停售套餐");
                    }
                });
            });
        }

        //正常情况下的停售与起售菜品操作
        this.lambdaUpdate().in(Dish::getId, ids).set(Dish::getStatus, status).update();
    }

    /**
     * 需要进行逻辑删除的信息有：菜品信息、菜品口味信息
     * 同时还需要判定，如果菜品目前正在起售中，那么不能删除
     * (由于只要菜品还在一个起售的套餐中，那么该菜品就无法停售，所以这里只要判断菜品是否起售即可，无需考虑与套餐的关系)
     *
     * @param ids 菜品id集合
     */
    @Override
    public void deleteWithFlavor(List<Long> ids) {
        //先查询菜品是否在起售中
        this.lambdaQuery().in(Dish::getId, ids).list().forEach(dish -> {
            if (dish.getStatus() == 1) {
                throw new ObjectStillOnStockException(dish.getName() + "目前还在起售中，无法删除，请考虑先停售菜品");
            }
        });
        // 逻辑删除菜品口味信息
        dishFlavorService.lambdaUpdate().in(DishFlavor::getDishId, ids).set(DishFlavor::getIsDeleted, 1).update();
        // 逻辑删除菜品信息
        this.lambdaUpdate().in(Dish::getId, ids).set(Dish::getIsDeleted, 1).update();
    }

    /**
     * 查询菜品分页数据
     *
     * @param pageWrapper 分页参数包装类
     * @param queryName   菜品名称
     */
    @Override
    public void getDishPage(Page<Dish> pageWrapper, String queryName) {
        this.lambdaQuery()
                .like(StringUtils.isNotBlank(queryName), Dish::getName, queryName)
                .eq(Dish::getIsDeleted, 0)
                .orderByDesc(Dish::getUpdateTime)
                .page(pageWrapper);
    }

    /**
     * 将菜品分页数据转为dto分页数据，dto中增加了口味数据与分类名称
     *
     * @param dishPage 菜品分页数据
     * @return 菜品dto分页数据
     */
    @Override
    public Page<DishDto> convertToDishDtoPage(Page<Dish> dishPage) {
        Page<DishDto> dishDtoPage = new Page<>();
        BeanUtils.copyProperties(dishPage, dishDtoPage, "records");
        List<DishDto> dishDtoList = dishPage.getRecords().stream().map(dish -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(dish, dishDto);
            dishDto.setCategoryName(categoryService.getById(dish.getCategoryId()).getName());
            dishDto.setFlavors(dishFlavorService.lambdaQuery().eq(DishFlavor::getDishId, dish.getId()).list());
            return dishDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(dishDtoList);
        return dishDtoPage;
    }


    /**
     * 根据分类查询菜品信息（同时根据菜品自身的sort属性与更新时间信息排序）
     *
     * @param queryCondition 查询条件包装类
     * @return 菜品信息
     */
    @Override
    public List<Dish> getDishListByCategory(Dish queryCondition) {
        return this.lambdaQuery()
                .eq(queryCondition.getCategoryId() != null, Dish::getCategoryId, queryCondition.getCategoryId())
                .eq(Dish::getStatus, 1) // 只查询起售中的菜品
                .eq(Dish::getIsDeleted, 0) // 只查询未删除的菜品
                .orderByAsc(Dish::getSort) // 根据sort属性排序
                .orderByDesc(Dish::getUpdateTime) // 根据更新时间排序
                .list();
    }

    /**
     * 将菜品信息转为DTO，dto中添加了口味信息
     *
     * @param dishList 菜品信息
     * @return 菜品信息DTO
     */
    @Override
    public List<DishDto> convertToDishDtoList(List<Dish> dishList) {
        return dishList.stream().map(dish -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(dish, dishDto);
            dishDto.setFlavors(dishFlavorService.lambdaQuery().eq(DishFlavor::getDishId, dish.getId()).list());
            return dishDto;
        }).collect(Collectors.toList());
    }
}
