package com.gagaco.xunxuproj2.service.user;

import com.gagaco.xunxuproj2.entity.User;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.web.dto.UserDto;

/**
 * 用户服务
 *
 */
public interface IUserService {

    User findUserByName(String username);

    ServiceResult<UserDto> findById(Long adminId);
}
