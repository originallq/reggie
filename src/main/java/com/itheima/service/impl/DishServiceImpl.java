package com.itheima.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.domain.Dish;
import com.itheima.domain.DishFlavor;
import com.itheima.dto.DishDto;
import com.itheima.mapper.DishMapper;
import com.itheima.service.DishFlavorService;
import com.itheima.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * @Description: 新增菜品, 同时保存口味数据
     * @Param: [dishDto]
     * @Return: void
     * @Author: Ling
     */
    @Override
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品的基本信息到Dish表中
        this.save(dishDto);

        //口味信息中没有菜品id数据,需要封装
        Long dishId = dishDto.getId();

        //保存口味信息到DishFlavor表中
        List<DishFlavor> flavors = dishDto.getFlavors();

        //为菜品口味对象属性dishId赋值;
        for (DishFlavor flavor : flavors) {
            flavor.setDishId(dishId);
        }
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * @Description: 根据id查询菜品信息和对应的口味信息
     * @Param: [id]
     * @Return: com.itheima.dto.DishDto
     * @Author: Ling
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息,从dish表中查
        Dish dish = this.getById(id);

        //创建DishDto对象,进行拷贝为其赋值
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);

        //根据菜品id(dishId)查询所有口味信息(list集合)
        LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();
        lqw.eq(DishFlavor::getDishId, dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(lqw);

        dishDto.setFlavors(flavors);

        return dishDto;
    }

    /**
     * @Description: 扩展修改菜品方法
     * @Param: [dishDto]
     * @Return: void
     * @Author: Ling
     */
    @Override
    public void updateWithFlavor(DishDto dishDto) {
        //修改dish表信息
        this.updateById(dishDto);

        //删除原先表中的口味信息---dish_flavor
        LambdaQueryWrapper<DishFlavor> lqw = new LambdaQueryWrapper<>();
        lqw.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(lqw);

        //将新增的口味信息插入dish_flavor表中
        List<DishFlavor> flavors = dishDto.getFlavors();
        //设置dishId
        flavors = flavors.stream().map((item) -> {
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);

    }
}
