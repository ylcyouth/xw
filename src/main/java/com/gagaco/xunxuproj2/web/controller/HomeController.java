package com.gagaco.xunxuproj2.web.controller;

import com.gagaco.xunxuproj2.base.ApiResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @time 2019-4-18
 * @time
 * @author wangjiajia
 *
 */

@Controller
public class HomeController {

    /*@GetMapping("/")
    public String index(Model model) {
        model.addAttribute("name", "thymeleaf from model from home controller");
        return "index";
    }*/

    @GetMapping("/get")
    @ResponseBody
    public ApiResponse get() {
        int a= 0;
        return ApiResponse.ofMessage(200, "请求成功了");
    }

    /*===================================================*/
    @GetMapping(value = {"/", "/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/403")
    public String accessError() {
        return "403";
    }

    @GetMapping("/404")
    public String notFoundPage() {
        return "404";
    }

    @GetMapping("/500")
    public String internalError() {
        return "500";
    }

    @GetMapping("/logout/page")
    public String logout() {
        return "logout";
    }





}
