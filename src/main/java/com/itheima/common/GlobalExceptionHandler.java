package com.itheima.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

//全局异常处理
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    //捕获添加用户名重复异常

    //@ExceptionHandler注解,表明捕获的异常类型
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> ExceptionHandler(SQLIntegrityConstraintViolationException ex) {
        log.error(ex.getMessage());
        //判断错误信息例是否包含重名错误 Duplicate entry
        if (ex.getMessage().contains("Duplicate entry")) {
            //切割错误信息,截取重名账号
            String[] split = ex.getMessage().split(" ");
            String msg = split[2] + "账号已存在";
            return R.error(msg);
        }
        return R.error("未知错误");
    }

    //捕获自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public R<String> ExceptionHandler(BusinessException ex) {
        log.info(ex.getMessage());
        return R.error(ex.getMessage());
    }
}
