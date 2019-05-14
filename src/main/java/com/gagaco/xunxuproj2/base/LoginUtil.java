package com.gagaco.xunxuproj2.base;

import com.gagaco.xunxuproj2.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @time 2019-4-27 11:02:28
 * @author wangjiajia
 */
public class LoginUtil {

    public static User load() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal != null && principal instanceof User) {
            return (User) principal;
        }
        return null;
    }

    public static Long getLoginUserId() {
        User user = load();
        if (user == null) {
            return -1L;
        }
        return user.getId();
    }







}
