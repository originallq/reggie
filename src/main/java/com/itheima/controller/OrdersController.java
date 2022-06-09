package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.BaseContext;
import com.itheima.common.R;
import com.itheima.domain.*;
import com.itheima.dto.OrdersDto;
import com.itheima.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("order")
public class OrdersController {
    @Autowired
    private OrdersService ordersService;
    @Autowired
    private OrderDetailService orderDetailService;
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private DishService dishService;
    @Autowired
    private SetmealService setmealService;

    /**
     * @Description: 服务器端订单明细分页查询
     * @Param: [page, pageSize, number, beginTime, endTime]
     * @Return
     */
    @GetMapping("/page")
    public R<Page> ordersDetails(int page, int pageSize, Long number, @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date beginTime, @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        //构造分页\条件构造器
        Page<Orders> ordersPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        //添加过滤条件
        //1.查询所有用户的订单明细,不是单独的某一个人
        //lqw.eq(Orders::getUserId, BaseContext.getCurrentId());
        //2.订单号,需要进行非空判断
        lqw.eq(number != null, Orders::getNumber, number);
        //3.开始时间&截止时间,需要进行非空判断
        lqw.between(beginTime != null && endTime != null, Orders::getCheckoutTime, beginTime, endTime);
        //4.按照订单时间排序
        lqw.orderByDesc(Orders::getOrderTime);

        //执行分页查询
        ordersService.page(ordersPage, lqw);

        return R.success(ordersPage);
    }

    /**
     * @Description: 修改派送状态
     * @Param: [orders]
     * @Return
     */
    @PutMapping
    public R<String> updateOrderStatus(@RequestBody Orders orders) {
        //传递过来的数据是已经修改好的状态数据,不需要在单独操作
        ordersService.updateById(orders);
        return R.success("修改成功");
    }


    /**
     * @Description: 客户端用户下单
     * @Param: [orders]
     * @Return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders, HttpServletRequest req) {
        ordersService.submit(orders, req);
        return R.success("下单成功");
    }

    /**
     * @Description: 客户端订单明细
     * @Param: [page, pageSize]
     * @Return
     */
    @GetMapping("/userPage")
    public R<Page> ordersList(int page, int pageSize) {
        Page<Orders> ordersPage = new Page<>(page, pageSize);
        //构建ordersDto分页构造器
        Page<OrdersDto> ordersDtoPage = new Page<OrdersDto>();

        LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        //查询当前用户订单
        lqw.eq(Orders::getUserId, BaseContext.getCurrentId());
        lqw.orderByDesc(Orders::getOrderTime);
        ordersService.page(ordersPage, lqw);

        /* 封装ordersDtoPage信息 */
        List<Orders> records = ordersPage.getRecords();
        //1.拷贝赋值,忽略records集合
        BeanUtils.copyProperties(ordersPage, ordersDtoPage, "records");

        //字符流处理方式
        List<OrdersDto> ordersDtoList = records.stream().map((item) -> {
            //拷贝赋值给ordersDto
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);

            //根据订单id,封装订单明细
            Long orderId = item.getId();
            LambdaQueryWrapper<OrderDetail> odLqw = new LambdaQueryWrapper<OrderDetail>();
            odLqw.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> orderDetails = orderDetailService.list(odLqw);
            ordersDto.setOrderDetails(orderDetails);

            return ordersDto;
        }).collect(Collectors.toList());

        //将封装好的ordersDtoList赋值给ordersDtoPage的records,用于展示数据
        ordersDtoPage.setRecords(ordersDtoList);

        return R.success(ordersDtoPage);
    }

    @PostMapping("/again")
    public R<String> again(@RequestBody Orders orders) {
        if (orders == null) {
            return R.error("未知错误");
        }
        //根据订单ID获取orders,以及订单下的菜品\套餐
        Orders order = ordersService.getById(orders.getId());

        LambdaQueryWrapper<OrderDetail> lqw = new LambdaQueryWrapper<>();
        lqw.eq(OrderDetail::getOrderId, orders.getId());
        //订单明细集合
        List<OrderDetail> list = orderDetailService.list(lqw);

        /*购物车数据回显,先判断安当前商品是菜品还是套餐,
        然后判断商品是否还在售卖中,如果在售卖,添加;否则去除*/
        for (int i = 0; i < list.size(); i++) {
            OrderDetail orderDetail = list.get(i);
            Long dishId = orderDetail.getDishId();
            if (dishId != null) {
                Dish dish = dishService.getById(dishId);
                if (dish.getStatus() == 0) {
                    list.remove(orderDetail);
                    i -= 1;
                }
            } else {
                Setmeal setmeal = setmealService.getById(orderDetail.getSetmealId());
                if (setmeal.getStatus() == 0) {
                    list.remove(orderDetail);
                    i -= 1;
                }
            }
        }

        //并发修改异常
        /*for (OrderDetail orderDetail : list) {
            Long dishId = orderDetail.getDishId();
            if (dishId != null) {
                Dish dish = dishService.getById(dishId);
                if (dish.getStatus() == 0) {
                    list.remove(orderDetail);
                }
            } else {
                Setmeal setmeal = setmealService.getById(orderDetail.getSetmealId());
                if (setmeal.getStatus() == 0) {
                    list.remove(orderDetail);
                }
            }
        }*/
        /*.filter((item) -> (
                    Long dishId = item.getDishId();
                    Long setmealId = item.getSetmealId();
                    Dish dish = dishService.getById(dishId);
                    Setmeal setmeal = setmealService.getById(setmealId);

        dishService.getById(item.getDishId()).getStatus() < 1 ||
        setmealService.getById(item.getSetmealId()).getStatus() < 1
        ))
        */

        List<ShoppingCart> shoppingCartList = list.stream().map((item) -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            //赋值拷贝
            BeanUtils.copyProperties(item, shoppingCart);
            shoppingCart.setUserId(BaseContext.getCurrentId());

            return shoppingCart;
        }).collect(Collectors.toList());

        //添加到购物车
        for (ShoppingCart shoppingCart : shoppingCartList) {
            shoppingCartService.save(shoppingCart);
        }

        return R.success("再来一单喽");
    }

}
