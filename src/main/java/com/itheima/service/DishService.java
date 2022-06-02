package com.itheima.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.domain.Dish;
import com.itheima.dto.DishDto;
import org.springframework.transaction.annotation.Transactional;

public interface DishService extends IService<Dish> {
    //扩展新增菜品方法
    @Transactional
    //开启事务,一般在接口方法上面写
    void saveWithFlavor(DishDto dishDto);

    //根据id查询菜品信息和对应的口味信息
    DishDto getByIdWithFlavor(Long id);

    //扩展修改菜品方法
    void updateWithFlavor(DishDto dishDto);

}
