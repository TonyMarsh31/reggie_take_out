package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<Object, Object> redisTemplate;

    public DishController(DishService dishService, RedisTemplate<Object, Object> redisTemplate) {
        this.dishService = dishService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        dishService.saveWithFlavor(dishDto);
        // mysql数据库中的数据发生变化，删除旧的缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1"; //_1 是status,表示菜品状态为上架
        redisTemplate.delete(key);
        return R.success("新增成功");
    }

    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        dishService.updateWithFlavor(dishDto);
        // mysql数据库中的数据发生变化，删除旧的缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);
        return R.success("修改成功");
    }

    @GetMapping("/page")
    public R<Page<DishDto>> page(int page, int pageSize, String name) {
        Page<Dish> dishPage = new Page<>(page, pageSize);
        dishService.getDishPage(dishPage, name);
        Page<DishDto> dishDtoPage = dishService.convertToDishDtoPage(dishPage);
        return R.success(dishDtoPage);
    }

    @GetMapping("/{id}")
    public R<DishDto> getById(@PathVariable Long id) {
        DishDto dishDto = dishService.getDishDtoWithFlavorById(id);
        return R.success(dishDto);
    }

    /**
     * 根据分类查询先关菜品信息
     * <p>
     * 先从Redis中获取缓存数据，存在则直接返回，没有则查询mysql，然后缓存并返回
     *
     * @param condition 封装的查询条件
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish condition) {
        //动态构造key  示例：dish_1397844391040167938_1
        String key = "dish_" + condition.getCategoryId() + "_" + condition.getStatus();
        List<DishDto> dishes = (List<DishDto>) redisTemplate.opsForValue().get(key);
        // Redis中存在数据，直接返回
        if (dishes != null) return R.success(dishes);
        // Redis中没有缓存，查询mysql
        List<Dish> dishListByCategory = dishService.getDishListByCategory(condition);
        // 将Dish对象转换为DishDto对象,其额外添加了口味数据
        List<DishDto> dishDtoList = dishService.convertToDishDtoList(dishListByCategory);
        // 将数据缓存到Redis中
        //TODO 需要设置过期时间吗？
        redisTemplate.opsForValue().set(key, dishDtoList);
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
