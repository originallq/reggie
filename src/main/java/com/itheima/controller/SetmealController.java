package com.itheima.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.R;
import com.itheima.domain.Category;
import com.itheima.domain.Setmeal;
import com.itheima.domain.SetmealCount;
import com.itheima.dto.SetmealDto;
import com.itheima.service.CategoryService;
import com.itheima.service.SetmealDishService;
import com.itheima.service.SetmealService;
import com.sun.xml.internal.fastinfoset.util.ValueArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {
    @Autowired
    private SetmealService setmealService;
    @Autowired
    private SetmealDishService setmealDishService;
    @Autowired
    private CategoryService categoryService;

    /**
     * @Description: 存储套餐信息和套餐菜品信息
     * @Param: [setmealDto]
     * @Return: com.itheima.common.R<java.lang.String>
     * @Author: Ling
     */
    @PostMapping
    // todo 删除该缓存名称(setmealCache)下的全部缓存
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> saveWithDish(@RequestBody SetmealDto setmealDto) {
        //使用扩展方法saveWithDish,存储套餐信息和套餐菜品信息
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    /**
     * @Description: 套餐分页查询
     * @Param: [page, pageSize, name]
     * @Return: com.itheima.common.R<com.baomidou.mybatisplus.extension.plugins.pagination.Page>
     * @Author: Ling
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        //1.构造分页构造器
        Page<Setmeal> pageSetmeal = new Page<>(page, pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>();

        //2.构造条件构造器
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
        lqw.like(name != null, Setmeal::getName, name);
        lqw.orderByDesc(Setmeal::getUpdateTime);
        //3.执行查询
        setmealService.page(pageSetmeal, lqw);

        /* 拓展功能,使的页面展示套餐分类 */
        //1.通过拷贝pageSetmeal给setmealDtoPage赋值,忽略已经展示在页面的信息list<> recoreds
        BeanUtils.copyProperties(pageSetmeal, setmealDtoPage, "records");
        //2.获取pageSetmeal的records
        List<Setmeal> records = pageSetmeal.getRecords();
        //3.进行处理,然后封装到setmealDtoPage中
        List<SetmealDto> list = records.stream().map((item) -> {
            // 1)创建新的SetmealDto对象,进行拷贝赋值
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);

            // 2)获取setmeal对象
            Long categoryId = item.getCategoryId();
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());//收集成list集合

        setmealDtoPage.setRecords(list);
        return R.success(setmealDtoPage);
    }

    /**
     * @Description: 根据id查询, 回显套餐信息与包含的菜品信息
     * @Param: [id]
     * @Return: com.itheima.common.R<com.itheima.dto.SetmealDto>
     */
    @GetMapping("/{id}")
    public R<SetmealDto> showSetmealInfo(@PathVariable Long id) {
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        return R.success(setmealDto);
    }

    /**
     * @Description: 修改套餐数据-->修改setmeal表和setmeal_dish表
     * @Param: []
     * @Return: com.itheima.common.R<java.lang.String>
     */
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        setmealService.updateWithDish(setmealDto);
        return R.success("修改成功");
    }


    /**
     * @Description: 删除套餐，同时需要删除套餐和菜品的关联数据
     * @Param: [ids]
     * @Return: com.itheima.common.R<java.lang.String>
     * @Author: Ling
     */
    @DeleteMapping
    // todo 删除该缓存名称(setmealCache)下的全部缓存
    @CacheEvict(value = "setmealCache",allEntries = true)
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids为{}", ids);
        setmealService.deleteWithDish(ids);
        return R.success("删除成功");
    }

    /**
     * @Description: 单个&批量停售/启售
     * @Param: [status, ids]
     * @Return: com.itheima.common.R<java.lang.String>
     * @Author: Ling
     */
    @PostMapping("/status/{status}")
    public R<String> updateSetmealStatus(@PathVariable int status, @RequestParam List<Long> ids) {
        for (Long id : ids) {
            //根据id查询当前套餐
            Setmeal setmeal = setmealService.getById(id);
            //设置状态
            setmeal.setStatus(status);
            setmealService.updateById(setmeal);
        }
        return R.success("修改状态成功");
    }

    /**
     * @Description: 根据分类id查询套餐
     * @Param: [setmeal]
     * @Return: com.itheima.common.R<java.util.List < com.itheima.domain.Setmeal>>
     */
    @GetMapping("/list")
    //todo 设置缓存
    @Cacheable(value = "setmealCache", key = "#setmeal.getCategoryId()+'_'+#setmeal.getStatus()")
    public R<List<Setmeal>> list(Setmeal setmeal) {
        LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
        lqw.eq(setmeal != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        lqw.eq(setmeal != null, Setmeal::getStatus, setmeal.getStatus());
        List<Setmeal> setmealList = setmealService.list(lqw);

        return R.success(setmealList);
    }

    @GetMapping("/count")
    public List<SetmealCount> count() {
        //查出setmeal集合
        List<Setmeal> list = setmealService.list();

        Set<Long> ids = new HashSet<>();
        for (Setmeal setmeal : list) {
            //获取分类id
            Long categoryId = setmeal.getCategoryId();
            ids.add(categoryId);
        }
        //方式二: 使用stream流去重,去除重复categoryId数据
//        List<Long> idList = list.stream().map((item) -> {
//            Long categoryId = item.getCategoryId();
//            return categoryId;
//        }).collect(Collectors.toList());

        List<SetmealCount> countList = new ArrayList<>();
        for (Long categoryId : ids) {
            //创建SetmealCount 对象
            SetmealCount setmealCount = new SetmealCount();

            //获取分类id
            //Long categoryId = setmeal.getCategoryId();
            //统计当前分类id下的套餐数量
            LambdaQueryWrapper<Setmeal> lqw = new LambdaQueryWrapper<>();
            lqw.eq(Setmeal::getCategoryId, categoryId);
            int count = setmealService.count(lqw);
            setmealCount.setValue(count); //填充数据

            //根据分类id获取分类名称
            Category category = categoryService.getById(categoryId);
            String categoryName = category.getName();
            setmealCount.setName(categoryName); //封装数据

            countList.add(setmealCount);
        }
        return countList;
        /*
        //2.将集合brands转成json字符串
        String res = JSON.toJSONString(countList);
        //3.将结果响应给浏览器
        //设置编码格式
        resp.setContentType("text/json;charset=utf-8");
        resp.getWriter().write(res);
        */

    }
}
