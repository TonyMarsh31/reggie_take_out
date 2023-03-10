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
    //TODO DishService和CategoryService形成了循环依赖，需要解决
    // 目前暂时的解决方案是使用@Autowired注解，使用Spring自己的三级缓存机制解决循环依赖
    // 但是这不是一个好的解决方案，因为循环依赖问题的本质是糟糕的程序设计，而这才是真正需要解决的问题
    // categoryService中的remove方法中需要dishService先查询该分类下是否有菜品，如果有则不允许删除
    // 而dishService中需要调用categoryService来查询菜品的分类名称来转换为DTO对象
    // 一个解决循环依赖的思路是将上述耦合的部分抽取到其他地方，让调用方调用该第三方,
    // 这样第三方直接依赖了这两个service，而不是两个service直接循环依赖
    // 解决方案1 : 将涉及到耦合部分的方法抽取到Controller层，然后在Controller层注入两个service
    // 解决方案2 : 将涉及到两个service耦合部分的方法抽取到一个新的service中，然后在这个新的service中注入两个service
    // 解决方法3 : 可以的话直接在Dao层扩展Mapper，进行多表的连接查询完成这个功能
    // 对于categoryService中的remove方法，可以考虑解决方法1
    // 对于entity对象转为dto对象的这种操作，可以考虑解决方法3，直接创建xml文件，进行多表连接查询后，将结果映射为dto对象
    // 但是这样会导致我们将无法使用mybatis-plus的一些特性，例如分页查询,lambdaQueryWrapper的使用等等
    // 对于DO到DTO的转换，还有一种解决方案4: 就是使用第三方工具类mapstruct来进行转换,这样也能解决问题。
    // 总之一个Service应当只依赖一个Dao(Mapper)，且符合单一职责原则，不推荐在Service中注入其他Service
    // 调用多个Service的逻辑应当出现在Controller层，
    // 如果该逻辑很复杂且需要多方复用，那么再考虑创建一个新的service来注入其他service
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
        save(dishDto);
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
        lambdaQuery()
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
        return lambdaQuery()
                .eq(queryCondition.getCategoryId() != null, Dish::getCategoryId, queryCondition.getCategoryId())
                .eq(Dish::getStatus, 1) // 只查询起售中的菜品
                .eq(Dish::getIsDeleted, 0) // 只查询未删除的菜品
                .orderByAsc(Dish::getSort) // 根据sort属性排序
                .orderByDesc(Dish::getUpdateTime) // 根据更新时间排序
                .list();
    }

    @Override
    public List<Dish> getDishListByMultiID(List<Long> ids) {
        return lambdaQuery()
                .in(ids.size() > 0, Dish::getId, ids)
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
