package com.itheima.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.domain.Category;

public interface CategoryService extends IService<Category> {
    //自定义一个扩展方法 --> 删除方法
    void remove(Long id);

}
