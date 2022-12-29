package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    private final DishService dishService;
    private final CategoryService categoryService;
    private final DishFlavorService dishFlavorService;


    public DishController(DishService dishService, CategoryService categoryService, DishFlavorService dishFlavorService) {
        this.dishService = dishService;
        this.categoryService = categoryService;
        this.dishFlavorService = dishFlavorService;
    }

    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info("dishDto:{}", dishDto);
        dishService.saveWithFlavor(dishDto);
        return R.success("新增成功");
    }

    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        log.info("dishDto:{}", dishDto);
        dishService.updateWithFlavor(dishDto);
        return R.success("修改成功");
    }

    @GetMapping("/page")
    public R<Page<DishDto>> page(int page, int pageSize, String name) {
        // 查询菜品信息
        Page<Dish> dishPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .like(name != null, Dish::getName, name)
                .orderByDesc(Dish::getUpdateTime)
                .eq(Dish::getIsDeleted, 0); // 仅展示未删除的菜品
        dishService.page(dishPage, queryWrapper);
        // 目前dishPage中的分类信息是分类ID，需要转换为分类名称
        // 先将page<DIsh>对象转换为page<DishDto>对象,使用对象拷贝工具类BeanUtils
        Page<DishDto> dishDtoPage = new Page<>();
        BeanUtils.copyProperties(dishPage, dishDtoPage, "records");
        // 然后单独处理records,将records中的每一个Dish对象转换为DishDto对象
        List<DishDto> dishDtoList = dishPage.getRecords().stream().map(dish -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(dish, dishDto);
            // jdk11后使用ifPresentOrElse方法更好一点,JDK8的orElse方法只能接受supplier
            Optional<Long> categoryID = Optional.ofNullable(dish.getCategoryId());
            categoryID.ifPresent(categoryId -> {
                // 用service根据id获取分类名称，然后设置分类名称
                String categoryName = categoryService.getById(categoryId).getName();
                dishDto.setCategoryName(categoryName);
            });
            categoryID.orElseThrow(() -> new RuntimeException("存在菜品没有分类"));
            return dishDto;
        }).collect(Collectors.toList());
        dishDtoPage.setRecords(dishDtoList);
        return R.success(dishDtoPage);
    }

    @GetMapping("/{id}")
    public R<DishDto> getById(@PathVariable Long id) {
        DishDto dishDto = dishService.getDishDtoWithFlavorById(id);
        return R.success(dishDto);
    }

    /**
     * 根据分类查询先关菜品信息
     *
     * @param condition 封装的查询条件
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish condition) {
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(condition.getCategoryId() != null, Dish::getCategoryId, condition.getCategoryId())
                .eq(Dish::getStatus, 1) // 仅查询上架的菜品
                .eq(Dish::getIsDeleted, 0) // 仅展示未逻辑删除的菜品
                .orderByAsc(condition.getSort() != null, Dish::getSort)
                .orderByDesc(condition.getUpdateTime() != null, Dish::getUpdateTime);
        List<Dish> dishList = dishService.list(queryWrapper);
        // 将Dish对象转换为DishDto对象,添加口味数据
        List<DishDto> dishDtoList = dishList.stream().map(dish -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(dish, dishDto);
            // 查询口味信息
            List<DishFlavor> dishFlavorList = dishFlavorService.list(
                    new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, dish.getId()));
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());
        return R.success(dishDtoList);
    }

    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
        log.info("status:{},ids:{}", status, ids);
        dishService.updateStatus(status, ids);
        return R.success("修改成功");
    }

    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        // 需要进行逻辑删除的信息有：菜品信息、菜品口味信息
        // 同时还需要判定，如果菜品目前正在起售中，那么不能删除
        dishService.deleteWithFlavor(ids);
        return R.success("删除套餐成功");
    }
}
