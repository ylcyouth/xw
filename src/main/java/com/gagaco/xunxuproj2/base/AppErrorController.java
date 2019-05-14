package com.gagaco.xunxuproj2.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @time 2019-4-20 23:02:42
 * @time
 * @author wangjiajia
 *
 * web错误的全局处理
 * ------------------------
 * 构造方法上贴 @Autowired 是啥意思？
 *
 * @ RequestMapping 的 produces属性
 *
 * ErrorAttributes类 ErrorController接口
 *
 * 在同一个Controller里面 多个方法的@ RequestMapping它的value属性值相同的话，spring mvc 会怎么处理呢？ 哈哈这个这个当成自己想的面试题，
 * 以后面试别人
 *
 * 对技术的贪心？ 痴心？
 * 只是想成为一个很厉害的程序员，碰见会用到的我不会的东西就想学，学了的又想学的很深，
 * 可能到最后会成为一个架构师，CTO，但是最初想法只是想成为一个很厉害的程序员
 *
 */
@Controller
public class AppErrorController implements ErrorController {

    private static final String ERROR_PATH = "/error";

    private ErrorAttributes errorAttributes;

    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }

    @Autowired
    public AppErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    /**
     * 处理web页面错误
     */
    @RequestMapping(value = ERROR_PATH, produces = "text/html")
    public String errorPageHandler(HttpServletResponse response) {
        int status = response.getStatus();
        //403,404,500错误返回对应的错误页面
        switch (status) {
            case 403:
                return "403";
            case 404:
                return "404";
            case 500:
                return "500";
        }
        //其他错误返回首页
        return "index";
    }


    /**
     * 处理除了web页面错误之外的错误
     */
    @RequestMapping(value = ERROR_PATH)
    @ResponseBody
    public ApiResponse apiErrorHandler(HttpServletRequest request) {
        RequestAttributes requestAttributes = new ServletRequestAttributes(request);
        Map<String, Object> errorAttributes = this.errorAttributes.getErrorAttributes(requestAttributes, false);
        int status = getStatus(request);
        return ApiResponse.ofMessage(status, String.valueOf(errorAttributes.getOrDefault("message", "error")));
    }

    private int getStatus(HttpServletRequest request) {
        Integer status = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (status != null) {
            //什么时候会等于null呢？
            return status;
        }
        return 500;
    }





}
