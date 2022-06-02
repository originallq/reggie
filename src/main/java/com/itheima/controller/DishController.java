package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.R;
import com.itheima.domain.Category;
import com.itheima.domain.Dish;
import com.itheima.domain.DishFlavor;
import com.itheima.dto.DishDto;
import com.itheima.service.CategoryService;
import com.itheima.service.DishFlavorService;
import com.itheima.service.DishService;
import jdk.nashorn.internal.ir.annotations.Ignore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    //注入CategoryService,根据id获取菜品名字
    private CategoryService categoryService;

    /**
     * @Description: 新增菜品->扩展方法
     * @Param: [dishDto]
     * @Return: com.itheima.common.R<java.lang.String>
     * @Author: Ling
     */
    @PostMapping
    //DTO: Data Transfer Object(数据传输对象)，一般用于展示层与服务层之间的数据传输。
    public R<String> save(@RequestBody DishDto dishDto) {
        //调用扩展方法saveWithFlavor
        dishService.saveWithFlavor(dishDto);
        return R.success("添加成功");
    }

    /**
     * @Description: 菜品分页查询
     * @Param: [page, pageSize, name]
     * @Return: com.itheima.common.R<com.baomidou.mybatisplus.extension.plugins.pagination.Page>
     * @Author: Ling
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //1.构造分页构造器
        Page<Dish> pageInfo = new Page(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        //2.构造条件构造器
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
        //添加过滤条件,模糊匹配
        lqw.like(name != null, Dish::getName, name);
        //按照价格降序
        lqw.orderByDesc(Dish::getUpdateTime);

        //3.执行分页操作
        dishService.page(pageInfo, lqw);

        /*
            4.对象拷贝,此时pageInfo中已经有数值
              将其拷贝到dishDtoPage,忽略已经展示在页面的数据List<T> records
        */
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        /* 5.获取pageInfo的records,进行处理过后,再赋值给dishDtoPage的records */
        List<Dish> records = pageInfo.getRecords();

        //方式一:for循环 遍历records集合(records = dishs)
        /*List<DishDto> list = new ArrayList<>();
        for (Dish record : records) {
            //copy(record),给新new出来的dishDto进行赋值
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(record, dishDto);

            //b.获取菜品分类id(categoryId)
            Long categoryId = record.getCategoryId();
            //c.注入CategoryService,根据id获取菜品名字
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            list.add(dishDto);
        }*/

        //方式二:stream流的方式
        List<DishDto> list = records.stream().map((item) -> {
            //1.创建DishDto对象,并进行拷贝赋值
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);
            //2.根据categoryId获取category对象
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            //3.根据category对象获取categoryName
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            return dishDto;
        }).collect(Collectors.toList());//收集成list集合

        dishDtoPage.setRecords(list);
        //返回dishDtoPage
        return R.success(dishDtoPage);
    }

    /**
     * @Description: 根据id查询菜品信息和对应的口味信息, 回显数据
     * @Param: [id]
     * @Return: com.itheima.common.R<com.itheima.dto.DishDto>
     * @Author: Ling
     */
    @GetMapping("/{id}")
    public R<DishDto> getByIdWithFlavor(@PathVariable long id) {
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * @Description: 修改菜品信息, 口味信息功能
     * @Param: [dishDto]
     * @Return: com.itheima.common.R<java.lang.String>
     * @Author: Ling
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        //调用saveWithFlavor方法
        dishService.updateWithFlavor(dishDto);
        return R.success("修改成功");
    }

    /**
     * @Description: 批量删除/单个删除操作,前端传来的是id数组
     * @Param: [ids]
     * @Return: com.itheima.common.R<java.lang.String>
     * @Author: Ling
     */
    @DeleteMapping
    public R<String> deleteIds(long[] ids) {
        for (long id : ids) {
            dishService.removeById(id);
        }
        return R.success("删除成功");
    }

    /**
     * @Description: 批量停售/起售
     * @Param: [status, ids]
     * @Return: com.itheima.common.R<java.lang.String>
     * @Author: Ling
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable int status, long[] ids) {
        for (long id : ids) {
            //根据id查询菜品信息
            Dish dish = dishService.getById(id);
            //改变当前菜品状态
            dish.setStatus(status);
            dishService.updateById(dish);
        }
        return R.success("操作成功");
    }

}
