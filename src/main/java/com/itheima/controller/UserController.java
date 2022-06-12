package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.common.R;
import com.itheima.domain.User;
import com.itheima.service.UserService;
import com.itheima.util.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

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
            //session.setAttribute(phone, code);

            //todo v1.0 将生成的验证码存入redis缓存中,有效时间5分钟
            redisTemplate.opsForValue().set(phone, code, 5, TimeUnit.MINUTES);

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
        //Object sessionCode = session.getAttribute(phone);

        //TODO v1.0 从redis中取出验证码
        Object sessionCode = redisTemplate.opsForValue().get(phone);

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
                //设置默认用户名,供订单明细使用
                String name = "用户" + ValidateCodeUtils.generateValidateCode(6);
                user.setName(name);
                userService.save(user);
            }
            //将登录信息存储到浏览器,避免拦截器拦截
            session.setAttribute("user", user.getId());

            //todo 登录成功后删除redis缓存的验证码
            redisTemplate.delete(phone);
            return R.success(user);
        }
        return R.error("登录失败");
    }

    @PostMapping("/loginout")
    public R<String> loginOut(HttpSession session) {
        //销毁浏览器中存储的session
        session.removeAttribute("user");
        return R.success("退出成功");
    }

}
