package com.itheima.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.domain.Setmeal;
import com.itheima.dto.SetmealDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    /*
    拓展方法,存储套餐信息到setmeal表,套餐菜品到setmeal_dish表
    多表操作,开启事务管理
    */
    @Transactional
    void saveWithDish(SetmealDto setmealDto);

    //删除套餐，同时需要删除套餐和菜品的关联数据
    @Transactional
    void deleteWithDish(List<Long> ids);
}
