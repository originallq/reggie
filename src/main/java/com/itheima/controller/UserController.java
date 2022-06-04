package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.common.R;
import com.itheima.domain.User;
import com.itheima.service.UserService;
import com.itheima.util.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * @Description: 发送短信验证码
     * @Param: [user, session]
     * @Return: com.itheima.common.R<java.lang.String>
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) {
        //1.获取手机号
        String phone = user.getPhone();
        //2.判断手机号是否为空
        if (phone != null) {
            //3.生成验证码
            String code = ValidateCodeUtils.generateValidateCode(6).toString();
            log.info("验证码:{}", code);
            //4.将生成的验证码保存到Session
            session.setAttribute(phone, code);

            return R.success("短信验证码发送成功");
        }
        return R.error("短信验证码发送失败");
    }

    /**
     * @Description: 用户登录
     * @Param: [user]
     * @Return: com.itheima.common.R<java.lang.String>
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody Map map, HttpSession session) {
        log.info(map.toString());
        //获取手机号,验证码
        String phone = map.get("phone").toString();
        String code = map.get("code").toString();
        //比较验证码是否正确
        Object sessionCode = session.getAttribute(phone);
        if (sessionCode != null && sessionCode.equals(code)) {
            //验证码相同,登录成功
            LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
            lqw.eq(User::getPhone, phone);
            User user = userService.getOne(lqw);
            //判断是否为新用户,如果则是存储到数据库
            if (user == null) {
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);
                userService.save(user);
            }
            //将登录信息存储到浏览器,避免拦截器拦截
            session.setAttribute("user", user.getId());
            return R.success(user);
        }
        return R.error("登录失败");
    }

}
