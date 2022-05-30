package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.common.R;
import com.itheima.domain.Employee;
import com.itheima.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    /**
     * @Description: 用户登录
     * @Author: Ling
     * @Param: [req, employee]
     * @Return: com.itheima.common.R<com.itheima.domain.Employee>
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest req, @RequestBody Employee employee) {
        //1 将页面提交的密码进行MD5加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2 根据用户名查询用户
        LambdaQueryWrapper<Employee> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(lqw);

        //3 判断emp是否为空
        if (emp == null) {
            return R.error("登录失败");
        }

        //4 判断密码是否正确
        if (!emp.getPassword().equals(password)) {
            return R.error("登录失败");
        }

        //5 判断状态是否被锁定
        if (emp.getStatus() == 0) {
            return R.error("账户已经被锁定,请联系管理员");
        }

        //6 登录成功，将员工id存入Session并返回登录成功结果
        req.getSession().setAttribute("employee", emp.getId());
        return R.success(emp);
    }

    /**
     * @Description: 退出登录
     * @Author: Ling
     * @Param: [req]
     * @Return: com.itheima.common.R<java.lang.String>
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest req) {
        //1 销毁session中存储的数据
        req.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }
}
