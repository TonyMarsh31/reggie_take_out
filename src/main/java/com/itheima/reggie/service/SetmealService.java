package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 保存套餐的基本信息，以及该套餐对应的菜品信息
     *
     * @param setmealDto 套餐信息
     */
    void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐的基本信息，以及该套餐对应的菜品信息(如有在售卖中的，则取消删除)
     *
     * @param ids 套餐主键列表
     */
    void deleteWithDish(List<Long> ids);

    /**
     * 更新套餐的基本信息，以及该套餐对应的菜品信息
     *
     * @param setmealDto 套餐信息dto
     */
    void updateWithDish(SetmealDto setmealDto);

    /**
     * 修改 停售、起售状态
     *
     * @param status 新的状态，1：起售，0: 停售
     * @param ids    套餐主键列表
     */
    void updateStatus(Integer status, List<Long> ids);
}
