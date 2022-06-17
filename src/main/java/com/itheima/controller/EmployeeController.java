package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.common.R;
import com.itheima.domain.Employee;
import com.itheima.domain.Goods;
import com.itheima.domain.Users;
import com.itheima.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * @Description: 添加员工
     * @Author: Ling
     * @Param: [req, emp]
     * @Return: com.itheima.common.R<java.lang.String>
     */
    @PostMapping
    public R<String> save(HttpServletRequest req, @RequestBody Employee emp) {
        //1 设置初始密码 123456,MD5加密
        emp.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        //2.设置创建,修改时间--> 自动填充
        //emp.setCreateTime(LocalDateTime.now());
        //emp.setUpdateTime(LocalDateTime.now());

        //3.获取当前系统登录用户的id Long类型 --> 自动填充
        //Long empId = (Long) req.getSession().getAttribute("employee");
        //emp.setUpdateUser(empId);
        //emp.setCreateUser(empId);

        //4.存储用户信息到数据库
        employeeService.save(emp);
        return R.success("新增成功");
    }

    /**
     * @Description: 员工信息分页查询
     * @Author: Ling
     * @Param: [page当前页, pageSize页面记录数, name查询员工条件]
     * @Return: com.itheima.common.R<com.baomidou.mybatisplus.extension.plugins.pagination.Page>
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        log.info("page={},pageSize={},name={}", page, pageSize, name);
        //1 构造分页构造器
        Page page1 = new Page(page, pageSize);

        //2.构造条件构造器
        LambdaQueryWrapper<Employee> lqw = new LambdaQueryWrapper<>();

        //3.添加过滤条件,模糊查询
        lqw.like(name != null, Employee::getName, name);
        //添加排序条件
        lqw.orderByDesc(Employee::getUpdateTime);

        //4.执行查询
        employeeService.page(page1, lqw);
        return R.success(page1);
    }

    /**
     * @Description: 禁用/启用员工状态
     * @Author: Ling
     * @Param: [req, emp]
     * @Return: com.itheima.common.R<java.lang.String>
     */
    @PutMapping()
    public R<String> updateById(HttpServletRequest req, @RequestBody Employee emp) {
        //1.获取登录该用户的id
        //Long empId = (Long) req.getSession().getAttribute("employee");

        //2.设置更新时间,更新用户 --> 自动填充
        //emp.setUpdateTime(LocalDateTime.now());
        //emp.setUpdateUser(empId);
        employeeService.updateById(emp);
        return R.success("修改成功");
    }

    /**
     * @Description: 修改用户, 先回显数据, 然后走的是updateById方法
     * @Author: Ling
     * @Param: [id]
     * @Return: com.itheima.common.R<com.itheima.domain.Employee>
     */
    @GetMapping("/{id}")
    public R<Employee> findById(@PathVariable Long id) {
        Employee emp = employeeService.getById(id);
        if (emp != null) {
            return R.success(emp);
        }
        return R.error("没有查到对应员工信息");
    }
}
