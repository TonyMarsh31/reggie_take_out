package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    private final SetmealService setmealService;
    private final CategoryService categoryService;
    private final SetmealDishService setmealDishService;

    public SetmealController(SetmealService setmealService, CategoryService categoryService, SetmealDishService setmealDishService) {
        this.setmealService = setmealService;
        this.categoryService = categoryService;
        this.setmealDishService = setmealDishService;
    }

    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息：{}", setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    @GetMapping("/page")
    public R<Page<SetmealDto>> page(int page, int pageSize, String name) {
        // 查询套餐的基础信息
        Page<Setmeal> setmealPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .like(StringUtils.isNotBlank(name), Setmeal::getName, name)
                .orderByDesc(Setmeal::getUpdateTime);
        setmealService.page(setmealPage, queryWrapper);
        //将setmealPage转换为setmealDtoPage,添加分类名称与嵌套的菜品信息
        Page<SetmealDto> setmealDtoPage = new Page<>();
        BeanUtils.copyProperties(setmealPage, setmealDtoPage, "records");
        List<SetmealDto> setmealDtoRecords = setmealPage.getRecords().stream()
                .map(setmeal ->
                {
                    SetmealDto setmealDto = new SetmealDto();
                    BeanUtils.copyProperties(setmeal, setmealDto);
                    setmealDto.setCategoryName(setmeal.getCategoryId() == null ? "无数据" : categoryService.getById(setmeal.getCategoryId()).getName());
                    LambdaQueryWrapper<SetmealDish> eq = new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, setmeal.getId());
                    setmealDto.setSetmealDishes(setmealDishService.list(eq));
                    return setmealDto;
                })
                .collect(Collectors.toList());
        setmealDtoPage.setRecords(setmealDtoRecords);
        return R.success(setmealDtoPage);
    }
}
