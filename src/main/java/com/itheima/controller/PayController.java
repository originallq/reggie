package com.itheima.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.common.BaseContext;
import com.itheima.domain.Orders;
import com.itheima.service.OrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.websocket.Session;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/alipay")
public class PayController {
    @Autowired
    private OrdersService ordersService;

    private final String APP_ID = "2021000120612692";
    //应用私钥
    private final String APP_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCIqhaJgK404DnTVXOM7VNSVolfm7l/ohLs7DWDv002HvD1Kn1IjYGqV3lZOhSmJbAb1ycGNK1dIZ8YVrkp6+e/Pmbxlm6bB+vrgkzGRxZZ5Jj6/AXZbD5bwkihygnnxwsFComIomM7sLc6CCL7dcMiMJJSbmkFFz2l6ebVBN/OmpLevMgCYcd3vlQ64+pJQ+V9wxQQT4DW2SVEHmaGfejD03e0G8VrdwTocJLk+qSRb/GwWW2CMIriYhcMt6y0xM3eH1o9t/IdTsYfhYpaeJ47xDpMJ8xhusegxGGU+YOPfdy79wFM4X2/9XIrKOFDjMUNnCxiKzo6ZXZrTT1qR+KpAgMBAAECggEAL8fPw1lDDueKMBIDKTVcWUA6HTwzuaTvmvfmqd47X32n3v9w9+nJeMXGfmSpDYHFtaYeVbQbHKGBnx/K8xYwWxO1KCfhGcDGQfu3XedCsEEVH0L3xJFsp+YgQLiDiA52EtghR60GprrdBKQNnv8ILy7uKXXCr+31Nsm1U6q1mOS7DaI2WTeI0TeFeBl9dMBnLDFUCV6P4bFf2s1A0DrzJMXFt+yviYDUOAd2gLysw6Z3jRM9SZa2f9yLanAqm+NMNbbPO9yBIi1P1R/iD2ixohhRc170Gwll7hjlCPI0Noou7/BkiRyZ7dfW3NXYjkQKMOzeHRs7n2XLMpgTMwtLUQKBgQDgwszOUoHknveCqCLW78VQnIMAtfDhx1fPpKnGlIiaZPrFKEn7jas+aG6pDGFoD3MZvhdcM3CCHzknkXUiMnzbYN/WbA9T6y8V0HNzZ3oPE5pJjlX4iaVLB6y42ScnhjxgfC/mdTmI4LZgY2u4nAmbSkCP16hi6N0LSD2/EE80NQKBgQCbqLuWNjGrfSVD1Fl8wAcilnRlD/PoeTfpnUxLr7qoDoybG/cREWBRyqfjnAeHEhoJGaIMVnK32rvadGS05cOG/yFns3NblnX203hlXfnG3cbMiMRkgh2WWHXX7c5B4jx/OxqX1j8ts92I/ED7KGqRIh+/bCmEtjG5D+ssZfzbJQKBgQC0IswcbDoYrmN2vyS1GRQOKHJCJo+5lJfHHpTHSBI3k8j8LO8mgoOFkBNovzhuOzunnReB2ORKFZYsJUM4tjglZv3fD1o1it+A30E4c+16md5lJ4O1nzPv72W5/KiJ3+cR+VY2oSsMjfT9SZFzMvPsFtWps1EMfM90FGEQeZTRJQKBgC0dm7ku3fiZXhqpyoIY2jiRKQ09sXEY6TbBy6JIPa6GAQTX9yUwRx2Nwll7GMdyXK90iX5P9BILyNTJVYME8UYIXnNDFNVf57LxmfyvXzzffMY9gcar281+uDEskNKRsv0hcVS+2TLy5evVSTRCg5irLk56GBKKCCFeVshYixAtAoGBAMDOkSA8exRUG3JTHZhjivBxKsEttNJrNWQyEFSsOVhIionIattkHHyx68ULosjTyHap9w2geSCJPhCFVJxm68u/6l+1/YsWBmlBF/mHeG6GlOMejUBOQqHPV50csnzFeiuBpA/FK5/DEeMBy+CXmkeCjqLalfhyldi1cqTs4H5Q";
    private final String CHARSET = "UTF-8";
    //支付宝公钥
    private final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn9VDuQW5xS5VuSY1COzjuTEGFRq/nV/XUZQt7yNqEF8TQyDQQGOdBodwdHIdQkJJlgh+nJtMkf2OfE3/J4vs9O0XRKCX0O+dcm8yMlIr/n5CFUoMTH6mkqvAT+N6f+aG9w1dgEcC6Q4Qisedwu5FMaEUY+s3lLFhtK9UVM4BQrwQFgSgqKaWJYlWDeT909XDPh/0AEwiw3HNNn5Ud1dTjvqof6BClxudNtr9nODwDxE7khQImYgsyH2zCnlcHJDbwTFxT4iuSm22E/et2oWPSHY9s3zjk5Jg5CwFCq4To2lWFL2KnCrlzQbqo5UinJ61AxPO5CoIG5a7DmIWxWuA4QIDAQAB";
    //这是沙箱接口路径,正式路径为https://openapi.alipay.com/gateway.do
    private final String GATEWAY_URL = "https://openapi.alipaydev.com/gateway.do";
    private final String FORMAT = "JSON";
    //签名方式
    private final String SIGN_TYPE = "RSA2";
    //支付宝异步通知路径,付款完毕后会异步调用本项目的方法,必须为公网地址
    private final String NOTIFY_URL = "http://127.0.0.1/notifyUrl";
    //支付宝同步通知路径,也就是当付款完毕后跳转本项目的页面,可以不是公网地址
    // ali:
    //private final String RETURN_URL = "http://127.0.0.1/alipay/returnUrl";
    // 付款成功后自动跳转至支付成功页面 :
    private final String RETURN_URL = "http://localhost/front/page/pay-success.html";


    //@RequestMapping("aliapy")
    @GetMapping
    public void alipay(HttpServletResponse httpResponse, HttpSession session) throws IOException {

        SecureRandom r = new SecureRandom();
        //实例化客户端,填入所需参数
        AlipayClient alipayClient = new DefaultAlipayClient(GATEWAY_URL, APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //在公共参数中设置回跳和通知地址
        request.setReturnUrl(RETURN_URL);
        request.setNotifyUrl(NOTIFY_URL);

        /* 通过用户ID&订单ID查出本次订单数据,设置订单编号,金额,商品信息 */
        //LambdaQueryWrapper<Orders> lqw = new LambdaQueryWrapper<>();
        //lqw.eq(Orders::getUserId, BaseContext.getCurrentId());
        //lqw.eq(Orders::getId, session.getAttribute("orderId"));
        Long orderId = (Long) session.getAttribute("orderId");
        Orders order = ordersService.getById(orderId);


        //商户订单号，商户网站订单系统中唯一订单号，必填
        //生成随机Id
        // ali: String out_trade_no = UUID.randomUUID().toString();
        String out_trade_no = order.getId().toString(); //数据库订单ID
        //付款金额，必填
        // ali:String total_amount = Integer.toString(r.nextInt(9999999) + 1000000);
        String total_amount = order.getAmount().toString();
        //订单名称，必填
        String subject = "菩提阁高端美食";
        //商品描述，可空
        String body = "尊敬的会员欢迎订购菩提阁高端美食";
        request.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
        String form = "";
        try {
            form = alipayClient.pageExecute(request).getBody(); // 调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        httpResponse.setContentType("text/html;charset=" + CHARSET);
        httpResponse.getWriter().write(form);// 直接将完整的表单html输出到页面
        httpResponse.getWriter().flush();
        httpResponse.getWriter().close();
    }

    //@RequestMapping(value = "/returnUrl", method = RequestMethod.GET)
    //@GetMapping("/returnUrl")
    public String returnUrl(HttpServletRequest request, HttpServletResponse response)
            throws IOException, AlipayApiException {
        System.out.println("=================================同步回调=====================================");

        // 获取支付宝GET过来反馈信息
        Map<String, String> params = new HashMap<String, String>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext(); ) {
            String name = (String) iter.next();
            String[] values = (String[]) requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            // 乱码解决，这段代码在出现乱码时使用
            valueStr = new String(valueStr.getBytes("utf-8"), "utf-8");
            params.put(name, valueStr);
        }

        System.out.println(params);//查看参数都有哪些
        boolean signVerified = AlipaySignature.rsaCheckV1(params, ALIPAY_PUBLIC_KEY, CHARSET, SIGN_TYPE); // 调用SDK验证签名
        //验证签名通过
        if (signVerified) {
            // 商户订单号
            String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "UTF-8");

            // 支付宝交易号
            String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"), "UTF-8");

            // 付款金额
            String total_amount = new String(request.getParameter("total_amount").getBytes("ISO-8859-1"), "UTF-8");

            System.out.println("商户订单号=" + out_trade_no);
            System.out.println("支付宝交易号=" + trade_no);
            System.out.println("付款金额=" + total_amount);

            //支付成功，修复支付状态
            //payService.updateById(Integer.valueOf(out_trade_no));
            return "ok";//跳转付款成功页面
        } else {
            return "no";//跳转付款失败页面
        }
    }
}


