package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.common.BaseContext;
import com.itheima.common.R;
import com.itheima.domain.ShoppingCart;
import com.itheima.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/shoppingCart")
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * @Description: 添加菜品/套餐购物车
     * @Param: [shoppingCart]
     * @Return: com.itheima.common.R<com.itheima.domain.ShoppingCart>
     */
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart) {
        log.info("购物车数据{}", shoppingCart);
        //1.设置用户id,指定是哪一个用户的购物车
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        //2.判断加入购物车的是菜品还是套餐
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId, currentId);

        Long dishId = shoppingCart.getDishId();
        if (dishId != null) {
            //是菜品
            lqw.eq(ShoppingCart::getDishId, dishId);
        } else {
            //是套餐
            lqw.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        //sql: select * from shopping_cart where user_id = ? and dish_id/setmeal_id = ?
        ShoppingCart cartServiceOne = shoppingCartService.getOne(lqw);
        if (cartServiceOne != null) {
            //表示购物车已经有菜品\套餐,默认数量number = 1,将其加 1
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number + 1);
            shoppingCartService.updateById(cartServiceOne);
        } else {
            //购物车没有该菜品\套餐,直接将shoppingCart存入
            shoppingCart.setNumber(1);
            shoppingCartService.save(shoppingCart);
            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);
    }

    /**
     * @Description: 查询当前用户id下的购物车数据
     * @Param: []
     * @Return: com.itheima.common.R<java.util.List < com.itheima.domain.ShoppingCart>>
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> list() {
        Long currentId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId, currentId);
        List<ShoppingCart> cartList = shoppingCartService.list(lqw);
        return R.success(cartList);
    }

    /**
     * @Description: 修改购物车菜品数量
     * @Param: [shoppingCart]
     * @Return: com.itheima.common.R<java.lang.String>
     */
    @PostMapping("/sub")
    public R<String> subtract(@RequestBody ShoppingCart shoppingCart) {
        //传递参数为 dishId,setmealId
        Long currentId = BaseContext.getCurrentId();

        //构造条件构造器
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        //添加过滤条件
        lqw.eq(ShoppingCart::getUserId, currentId);
        if (shoppingCart.getDishId() != null) {
            //修改的是菜品数量
            lqw.eq(ShoppingCart::getDishId, shoppingCart.getDishId());
        } else {
            //修改的是套餐数量
            lqw.eq(ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        }
        ShoppingCart cartOne = shoppingCartService.getOne(lqw);
        if (cartOne != null) {
            Integer number = cartOne.getNumber();
            //如果number为 1,执行操作后立刻删除该菜品
            if (number == 1) {
                //cartOne.setNumber(number - 1);
                //shoppingCartService.updateById(cartOne);
                shoppingCartService.remove(lqw);
            } else {
                //将菜品数量减 1
                cartOne.setNumber(number - 1);
                shoppingCartService.updateById(cartOne);
            }
        }
        return R.success("操作成功");
    }

    @DeleteMapping("/clean")
    public R<String> delete() {
        //根据当前用户id清空购物车
        Long currentId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ShoppingCart::getUserId, currentId);
        shoppingCartService.remove(lqw);
        return R.success("清空购物车成功");
    }
}
