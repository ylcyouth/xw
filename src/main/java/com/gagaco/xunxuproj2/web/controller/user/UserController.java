package com.gagaco.xunxuproj2.web.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @time 2019-4-21 20:55:23
 * @author wangjiajia
 *
 */
@Controller
public class UserController {


    @GetMapping("/user/login")
    public String userLoginPage() {
        return "user/login";
    }

    @GetMapping("/user/center")
    public String userCenterPage() {
        return "user/center";
    }


}
