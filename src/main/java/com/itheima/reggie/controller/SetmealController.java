package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
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
    private final DishService dishService;

    public SetmealController(SetmealService setmealService, CategoryService categoryService, SetmealDishService setmealDishService, DishService dishService) {
        this.setmealService = setmealService;
        this.categoryService = categoryService;
        this.setmealDishService = setmealDishService;
        this.dishService = dishService;
    }

    /**
     * 根据分类查询套餐
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId())
                .eq(Setmeal::getIsDeleted, 0)
                .eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        List<Setmeal> setmealList = setmealService.list(queryWrapper);
        return R.success(setmealList);
    }

    /**
     * 查询一个套餐下的具体菜品信息
     *
     * @param setmeal 封装的查询条件
     */
    // todo 不知道前端的请求路径，暂时空置
    public R<List<Dish>> getDish(Setmeal setmeal) {
        // 1.查关系表，获取套餐绑定的菜品ID
        List<SetmealDish> setmealDishList = setmealDishService.list(new LambdaQueryWrapper<SetmealDish>()
                .eq(setmeal.getId() != null, SetmealDish::getSetmealId, setmeal.getId()));
        List<Long> dishIds = setmealDishList.stream().map(SetmealDish::getDishId).collect(Collectors.toList());
        // 2.根据菜品ID查询菜品信息
        List<Dish> dishList = dishService.list(new LambdaQueryWrapper<Dish>().in(Dish::getId, dishIds));
        return R.success(dishList);
    }

    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息：{}", setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }


    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息：{}", setmealDto);
        setmealService.updateWithDish(setmealDto);
        return R.success("修改套餐成功");
    }

    /**
     * 根据id查询套餐信息，包裹嵌套的菜品信息等等，用于前端修改页面回显
     */
    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id) {
        Setmeal setmeal = setmealService.getById(id);
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);
        List<SetmealDish> setmealDishList = setmealDishService.list(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, id));
        setmealDto.setSetmealDishes(setmealDishList);
        setmealDto.setCategoryName(categoryService.getById(setmeal.getCategoryId()).getName());
        return R.success(setmealDto);
    }


    @GetMapping("/page")
    public R<Page<SetmealDto>> page(int page, int pageSize, String name) {
        // 查询套餐的基础信息
        Page<Setmeal> setmealPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .like(StringUtils.isNotBlank(name), Setmeal::getName, name)
                .orderByDesc(Setmeal::getUpdateTime)
                .eq(Setmeal::getIsDeleted, 0);
        setmealService.page(setmealPage, queryWrapper);
        //将setmealPage转换为setmealDtoPage,添加分类名称与嵌套的菜品信息
        Page<SetmealDto> setmealDtoPage = new Page<>();
        BeanUtils.copyProperties(setmealPage, setmealDtoPage, "records");
        List<SetmealDto> setmealDtoRecords = setmealPage.getRecords().stream()
                .map(setmeal ->
                {
                    // 发现在前端(分页列表中)不需要展示嵌套的菜品信息，所以这里可以不用查询相关信息，只需要查询分类名称即可
                    SetmealDto setmealDto = new SetmealDto();
                    BeanUtils.copyProperties(setmeal, setmealDto);
                    setmealDto.setCategoryName(setmeal.getCategoryId() == null ? "无数据" : categoryService.getById(setmeal.getCategoryId()).getName());
                    // 但是相关代码先暂时保留
                    LambdaQueryWrapper<SetmealDish> eq = new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, setmeal.getId());
                    setmealDto.setSetmealDishes(setmealDishService.list(eq));
                    return setmealDto;
                })
                .collect(Collectors.toList());
        setmealDtoPage.setRecords(setmealDtoRecords);
        return R.success(setmealDtoPage);
    }


    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        // 需要删除 1.套餐信息 2.套餐与菜品的关系信息
        setmealService.deleteWithDish(ids);
        return R.success("删除套餐成功");
    }


    /**
     * 修改 停售、起售状态
     *
     * @param status 新的状态，1: 起售，0: 停售
     * @param ids    套餐id集合
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
        log.info("status:{},ids:{}", status, ids);
        setmealService.updateStatus(status, ids);
        return R.success("修改成功");
    }
}
