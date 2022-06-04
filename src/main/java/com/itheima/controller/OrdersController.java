package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.BaseContext;
import com.itheima.common.R;
import com.itheima.domain.OrderDetail;
import com.itheima.domain.Orders;
import com.itheima.dto.OrdersDto;
import com.itheima.service.OrderDetailService;
import com.itheima.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    /**
     * @Description: 用户下单
     * @Param: [orders]
     * @Return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        ordersService.submit(orders);
        return R.success("下单成功");
    }

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

}
