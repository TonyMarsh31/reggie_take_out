package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.exception.ObjectContainsNestedProperties;
import com.itheima.reggie.mapper.CategoryMapper;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishService;
import com.itheima.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final SetmealService setmealService;
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
    private DishService dishService;

    public CategoryServiceImpl(SetmealService setmealService) {
        this.setmealService = setmealService;
    }

    /**
     * 根据id删除分类，删除前进行判断:如果分类下有菜品或套餐，不允许删除
     *
     * @param id 分类id
     */
    @Override
    public void remove(Long id) {
        //先查询该分类下是否有菜品或套餐
        LambdaQueryWrapper<Dish> dishQueryWrapper = new LambdaQueryWrapper<>();
        dishQueryWrapper.eq(Dish::getCategoryId, id);
        int count = dishService.count(dishQueryWrapper);
        if (count > 0) {
            throw new ObjectContainsNestedProperties("该分类下已有菜品，无法进行删除");
        }
        LambdaQueryWrapper<Setmeal> setmealQueryWrapper = new LambdaQueryWrapper<>();
        setmealQueryWrapper.eq(Setmeal::getCategoryId, id);
        int count1 = setmealService.count(setmealQueryWrapper);
        if (count1 > 0) {
            throw new ObjectContainsNestedProperties("该分类下已有套餐，无法进行删除");
        }
        //如果没有，就删除
        baseMapper.deleteById(id);
    }
}
