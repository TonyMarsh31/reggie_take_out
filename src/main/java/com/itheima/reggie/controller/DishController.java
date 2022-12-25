package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
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
    private final DishFlavorService dishFlavorService;
    private final CategoryService categoryService;


    public DishController(DishService dishService, DishFlavorService dishFlavorService, CategoryService categoryService) {
        this.dishService = dishService;
        this.dishFlavorService = dishFlavorService;
        this.categoryService = categoryService;
    }

    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info("dishDto:{}", dishDto);
        dishService.saveWithFlavor(dishDto);
        return R.success("新增成功");
    }

    @GetMapping("/page")
    public R<Page<DishDto>> page(int page, int pageSize, String name) {
        // 查询菜品信息
        Page<Dish> dishPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null, Dish::getName, name).orderByDesc(Dish::getUpdateTime);
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
}
