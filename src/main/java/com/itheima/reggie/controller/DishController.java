package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    //region 管理端API
    // 后台操作并发量不大，不需要缓存，但在CRUD操作后需要清除缓存

    /**
     * 管理端添加新的菜品，同时向菜品和菜品口味表中添加数据
     * <p>
     * 添加完成后，清除该分类下的菜品Redis缓存
     *
     * @param dishDto 菜品和口味信息
     */
    @CacheEvict(value = "dishListByCategory", key = "#dishDto.categoryId")
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        dishService.saveWithFlavor(dishDto);
        return R.success("新增成功");
    }

    /**
     * 管理端删除菜品信息，同时删除菜品和菜品口味表中的数据
     *
     * @param ids 菜品id
     */
    @CacheEvict(value = "dishListByCategory", allEntries = true)
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        // 需要进行逻辑删除的信息有：菜品信息、菜品口味信息
        // 同时还需要判定，如果菜品目前正在起售中，那么不能删除
        dishService.deleteWithFlavor(ids);
        return R.success("删除套餐成功");
    }

    /**
     * 管理端更新菜品信息，同时更新菜品和菜品口味表中的数据
     *
     * @param dishDto 菜品和口味信息
     */
    @CacheEvict(value = "dishListByCategory", key = "#dishDto.categoryId")
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        dishService.updateWithFlavor(dishDto);
        return R.success("修改成功");
    }

    /**
     * 管理端更改菜品的状态： 起售|停售
     *
     * @param status 状态
     * @param ids    菜品id
     */
    @CacheEvict(value = "dishListByCategory", allEntries = true)
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
        dishService.updateStatus(status, ids);
        return R.success("修改成功");
    }

    /**
     * 管理端根据主键查询菜品详细信息，一般是在修改菜品信息时使用
     *
     * @param id 菜品id
     */
    @GetMapping("/{id}")
    public R<DishDto> getById(@PathVariable Long id) {
        DishDto dishDto = dishService.getDishDtoWithFlavorById(id);
        return R.success(dishDto);
    }

    /**
     * 管理端分页查询菜品信息
     */
    @GetMapping("/page")
    public R<Page<DishDto>> page(int page, int pageSize, String name) {
        Page<Dish> dishPage = new Page<>(page, pageSize);
        dishService.getDishPage(dishPage, name);
        Page<DishDto> dishDtoPage = dishService.convertToDishDtoPage(dishPage);
        return R.success(dishDtoPage);
    }
    //endregion

    //region 用户端API

    /**
     * 用户端根据分类查询相关菜品信息
     * <p>
     * 先从Redis中获取缓存数据，存在则直接返回，没有则查询mysql，然后缓存并返回
     *
     * @param condition 封装的查询条件
     */
    @Cacheable(value = "dishListByCategory", key = "#condition.categoryId")
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish condition) {
        List<Dish> dishListByCategory = dishService.getDishListByCategory(condition);
        // 将Dish对象转换为DishDto对象,其额外添加了口味数据
        List<DishDto> dishDtoList = dishService.convertToDishDtoList(dishListByCategory);
        return R.success(dishDtoList);
    }

    //endregion
}
