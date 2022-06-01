package com.itheima.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.common.BusinessException;
import com.itheima.domain.Category;
import com.itheima.domain.Dish;
import com.itheima.domain.Setmeal;
import com.itheima.mapper.CategoryMapper;
import com.itheima.service.CategoryService;
import com.itheima.service.DishService;
import com.itheima.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;

    /**
     * @Description: 扩展删除方法, 判断菜品\套餐分类是否关联dish,setmeal
     * @Param: [id]
     * @Return: void
     * @Author: Ling
     */
    @Override
    public void remove(Long id) {
        /* 查询当前分类是否关联了菜品,如果关联,抛出一个业务异常 */
        //1.构造条件构造器
        LambdaQueryWrapper<Dish> dishLqw = new LambdaQueryWrapper<>();
        //2.添加过滤条件,根据分类categoryId进行查询
        dishLqw.eq(Dish::getCategoryId, id);
        int count1 = dishService.count(dishLqw);
        //3.count>0表明有关联
        if (count1 > 0) {
            //抛出一个业务异常,然后全局异常捕获
            throw new BusinessException("当前分类下关联了菜品,无法删除");
        }

        /* 查询当前分类是否关联了套餐,如果关联,抛出一个业务异常 */
        //1.构造条件构造器
        LambdaQueryWrapper<Setmeal> setmealLqw = new LambdaQueryWrapper<>();
        //2.添加过滤条件,根据分类categoryId进行查询
        setmealLqw.eq(Setmeal::getCategoryId, id);
        int count2 = setmealService.count(setmealLqw);
        //3.count>0表明有关联
        if (count2 > 0) {
            //抛出一个业务异常
            throw new BusinessException("当前分类下关联了套餐,无法删除");

        }

        /* 正常删除分类 */
        //调用父类removeById方法
        super.removeById(id);


    }
}
