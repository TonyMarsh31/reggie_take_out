package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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


    public DishController(DishService dishService, DishFlavorService dishFlavorService) {
        this.dishService = dishService;
        this.dishFlavorService = dishFlavorService;
    }

    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        dishService.saveWithFlavor(dishDto);
        return R.success("新增成功");
    }

    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        dishService.updateWithFlavor(dishDto);
        return R.success("修改成功");
    }

    @GetMapping("/page")
    public R<Page<DishDto>> page(int page, int pageSize, String name) {
        Page<Dish> dishPage = new Page<>(page, pageSize);
        dishService.getDishPage(dishPage, name);
        Page<DishDto> dishDtoPage = dishService.convertDishPageToDishDtoPage(dishPage);
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
        List<Dish> dishList = dishService
                .lambdaQuery()
                .eq(condition.getCategoryId() != null, Dish::getCategoryId, condition.getCategoryId())
                .eq(Dish::getStatus, 1) // 仅查询上架的菜品
                .eq(Dish::getIsDeleted, 0) // 仅展示未逻辑删除的菜品
                .orderByAsc(condition.getSort() != null, Dish::getSort)
                .orderByDesc(condition.getUpdateTime() != null, Dish::getUpdateTime)
                .list();
        // 将Dish对象转换为DishDto对象,添加口味数据
        List<DishDto> dishDtoList = dishList.stream().map(dish -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(dish, dishDto);
            // 查询口味信息
            List<DishFlavor> dishFlavorList = dishFlavorService
                    .lambdaQuery()
                    .eq(DishFlavor::getDishId, dish.getId())
                    .list();
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());
        return R.success(dishDtoList);
    }

    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
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
