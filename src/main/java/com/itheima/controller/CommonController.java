package com.itheima.controller;

import com.itheima.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;

@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    //yml语法
    @Value("${reggie.path}")
    private String basePath;

    /**
     * @Description: 文件上传
     * @Param: [file]
     * @Return: com.itheima.common.R<java.lang.String>
     * @Author: Ling
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file) {
        //file是一个临时文件，需要转存到指定位置，否则本次请求完成后临时文件会删除
        log.info(file.toString());

        //原始文件名,截取后缀名(suffix)
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        //使用UUID重新生成文件名,防止文件名重复
        String fileName = UUID.randomUUID().toString() + suffix;

        //创建一个目录对象
        File dir = new File(basePath);
        //判断目录是否存在
        if (!dir.exists()) {
            //不存在,则创建出来
            dir.mkdirs();
        }

        //1.将临时文件转存到磁盘
        try {
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //响应文件名,供下载时候使用
        return R.success(fileName);
    }

    /**
     * @Description: 文件下载
     * @Param: [name, resp]
     * @Return: void
     * @Author: Ling
     */
    @GetMapping("/download")
    //前端传递的数据为?name=***,所以参数必须为name
    public void download(String name, HttpServletResponse resp) {
        try {
            //1.创建输入流读取文件内容
            FileInputStream fis = new FileInputStream(new File(basePath + name));
            //2.创建输出流将文件写回浏览器
            ServletOutputStream os = resp.getOutputStream();

            //3.通过response对象设置响应数据格式(image/jpeg)
            resp.setContentType("image/jpeg");

            //4.每次读取1024个字节
            byte[] bys = new byte[1024];
            int len;
            while ((len = fis.read(bys)) != -1) {
                os.write(bys, 0, len);
                os.flush();
            }
            //关流
            os.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
