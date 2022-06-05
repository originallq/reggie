package com.itheima.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.domain.Orders;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;

public interface OrdersService extends IService<Orders> {
    /**
     * @Description: 用户下单
     * @Param: [orders]
     * @Return: void
     */
    @Transactional
    void submit(Orders orders, HttpSession session);
}
