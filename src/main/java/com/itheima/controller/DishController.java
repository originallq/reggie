package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.R;
import com.itheima.domain.Category;
import com.itheima.domain.Dish;
import com.itheima.domain.DishCount;
import com.itheima.domain.DishFlavor;
import com.itheima.dto.DishDto;
import com.itheima.service.CategoryService;
import com.itheima.service.DishFlavorService;
import com.itheima.service.DishService;
import jdk.nashorn.internal.ir.annotations.Ignore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
    @Autowired
    private RedisTemplate redisTemplate;

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
        //todo 全部删除 如果有新保存数据,则把缓存全部删除
        //Set<String> keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);
        //todo 精确删除 保存在那个分类下,就删除该分类的缓存数据
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

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

        //todo 全部清理 修改数据后需要清理缓存,保证数据的一致性
        //Set<String> keys = redisTemplate.keys("dish_*");
        //redisTemplate.delete(keys);
        //todo 精确清理: 修改哪一个分类就清理这个分类的缓存,不全部清理
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("修改成功");
    }

    /**
     * @Description: 批量删除/单个删除操作,前端传来的是id集合
     * @Param: [ids]
     * @Return: com.itheima.common.R<java.lang.String>
     * @Author: Ling
     */
    @DeleteMapping
    public R<String> deleteIds(@RequestParam List<Long> ids) {
        //使用扩展方法deleteWithFlavor,同时删除菜品和对应的口味数据
        dishService.deleteWithFlavor(ids);
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

    /**
     * @Description: 新增套餐中选择套餐下拉框
     * @Param: [dish]
     * @Return: com.itheima.common.R<java.util.List < com.itheima.domain.Dish>>
     * @Author: Ling
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        //todo v1.0 判断redis缓存中是否有菜品数据,如果有直接从缓存中取数据
        List<DishDto> dishDtoList = null;
        //todo v1.0 动态构造key dish_******_1
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        //todo v1.0 先从redis中获取缓存数据
        dishDtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);
        if (dishDtoList != null) {
            //TODO 如果有直接返回数据
            return R.success(dishDtoList);
        }
        //todo 如果没有则从数据库中查询,再存入redis中

        //1.构造条件构造器
        LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();

        //2.添加过滤条件
        lqw.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        //只查询状态为1(起售状态)的菜品
        lqw.eq(Dish::getStatus, 1);
        lqw.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        //3.执行sql
        List<Dish> list = dishService.list(lqw);

        /* 扩展:返回集合中应该含有口味信息,用于展示 */

        dishDtoList = list.stream().map((item) -> {
            /* 1.封装分类名称 */
            //a.创建DishDto对象,并进行拷贝赋值
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);

            //b.获得分类id->categoryId
            Long categoryId = dishDto.getCategoryId();
            //c.根据分类id获取分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }

            /* 2.封装口味信息 */
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> dfLqw = new LambdaQueryWrapper<>();
            dfLqw.eq(DishFlavor::getDishId, dishId);
            List<DishFlavor> dishFlavorList = dishFlavorService.list(dfLqw);
            dishDto.setFlavors(dishFlavorList);


            return dishDto;
        }).collect(Collectors.toList());
        //todo 设置过期时间为60分钟
        redisTemplate.opsForValue().set(key, dishDtoList, 60, TimeUnit.MINUTES);

        return R.success(dishDtoList);
    }


    /**
     * @Description: 数据视图展示
     * @Param: []
     * @Return
     */
    @GetMapping("/count")
    public List<DishCount> count() {
        //1.获取dish的集合
        List<Dish> list = dishService.list();
        //2.使用set集合去除重复分类id
        Set<Long> ids = new HashSet<>();
        for (Dish dish : list) {
            Long categoryId = dish.getCategoryId();
            ids.add(categoryId);
        }
        //3.使用id获取分类名称和每个名称对应下的菜品数量
        List<DishCount> dishCountList = ids.stream().map((item) -> {
            //此时item代表的是分类ID(categoryId)
            DishCount dishCount = new DishCount();

            //封装value值
            LambdaQueryWrapper<Dish> lqw = new LambdaQueryWrapper<>();
            lqw.eq(Dish::getCategoryId, item);
            int count = dishService.count(lqw);
            dishCount.setValue(count);

            //封装name值
            Category category = categoryService.getById(item);
            dishCount.setName(category.getName());

            return dishCount;
        }).collect(Collectors.toList());

        return dishCountList;
    }

}
