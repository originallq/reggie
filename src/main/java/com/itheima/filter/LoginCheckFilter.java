package com.itheima.filter;

import com.alibaba.fastjson.JSON;
import com.itheima.common.BaseContext;
import com.itheima.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.DigestUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
//过滤所有
@WebFilter("/*")
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;

        //1 获取当前请求的路径
        String requestURI = req.getRequestURI();
        log.info("本次请求路径:{}", requestURI);

        //2 定义不需要执行操作的路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/login",
                "/user/sendMsg"
        };

        //3 判断本次是否需要执行操作
        boolean check = check(urls, requestURI);

        //4 匹配成功,放行
        if (check) {
            log.info("路径:{}放行", requestURI);
            filterChain.doFilter(req, resp);
            return;
        }

        //5-1 判断用户是否已经登录
        if (req.getSession().getAttribute("employee") != null) {
            log.info("Id为{}的用户已经登录", req.getSession().getAttribute("employee"));
            //long id = Thread.currentThread().getId();
            //log.info("当前线程id为:{}", id);

            //将当前登录用户的id存入到ThreadLocal中
            Long empId = (Long) req.getSession().getAttribute("employee");
            BaseContext.setCurrentId(empId);

            filterChain.doFilter(req, resp);
            return;
        }
        //5-2 判断移动端用户是否已经登录
        if (req.getSession().getAttribute("user") != null) {
            log.info("Id为{}的用户已经登录", req.getSession().getAttribute("user"));
            //long id = Thread.currentThread().getId();
            //log.info("当前线程id为:{}", id);

            //将当前登录用户的id存入到ThreadLocal中
            Long userId = (Long) req.getSession().getAttribute("user");
            BaseContext.setCurrentId(userId);

            filterChain.doFilter(req, resp);
            return;
        }

        log.info("用户未登录,跳转至登录界面");
        resp.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;

    }

    /**
     * @Description: 匹配路径
     * @Author: Ling
     * @Param: [urls, requestURI]
     * @Return: boolean
     */
    public boolean check(String[] urls, String requestURI) {
        for (String url : urls) {
            //路径匹配成功返回 true
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) {
                return true;
            }
        }
        return false;
    }
}
