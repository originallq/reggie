package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.R;
import com.itheima.domain.Category;
import com.itheima.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * @Description: 新增菜品分类
     * @Param: [category]
     * @Return: com.itheima.common.R<java.lang.String>
     * @Author: Ling
     */
    @PostMapping
    public R<String> saveDishCategory(@RequestBody Category category) {
        //1 操作界面接受的json数据
        categoryService.save(category);
        return R.success("添加成功");
    }

    /**
     * @Description: 菜品\套餐分页查询
     * @Param: [page, pageSize]
     * @Return: com.itheima.common.R<com.baomidou.mybatisplus.extension.plugins.pagination.Page>
     * @Author: Ling
     */
    @GetMapping("/page")
    public R<Page> page(Integer page, Integer pageSize) {
        log.info("page={},pageSize={}", page, pageSize);
        //1 构造分页构造器
        Page pageInfo = new Page(page, pageSize);

        //2 构造条件构造器
        LambdaQueryWrapper<Category> lqw = new LambdaQueryWrapper<>();
        lqw.orderByAsc(Category::getSort);

        //3 执行分页查询
        categoryService.page(pageInfo, lqw);
        return R.success(pageInfo);
    }

    /**
     * @Description: 修改菜品\套餐分类
     * @Param: [category]
     * @Return: com.itheima.common.R<java.lang.String>
     * @Author: Ling
     */
    @PutMapping
    public R<String> update(@RequestBody Category category) {
        //1.公共字段自动填充

        //2.执行修改操作-->category包涵(type,name,sort)
        categoryService.updateById(category);
        return R.success("修改成功");
    }

    @DeleteMapping()
    public R<String> delete(Long id) {
        //执行自定义删除操作
        categoryService.remove(id);
        return R.success("删除成功");
    }
}
