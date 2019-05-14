package com.gagaco.xunxuproj2.service.user.impl;

import com.gagaco.xunxuproj2.entity.Role;
import com.gagaco.xunxuproj2.entity.User;
import com.gagaco.xunxuproj2.repository.RoleRepository;
import com.gagaco.xunxuproj2.repository.UserRepository;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.service.user.IUserService;
import com.gagaco.xunxuproj2.web.dto.UserDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @time 2019-4-21 19:37:30
 * @author wangjiajia
 *
 *
 */
@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ModelMapper modelMapper;



    @Override
    public User findUserByName(String username) {
        User user = userRepository.findByName(username);
        if (user == null) {
            return null;
        }

        List<Role> roleList = roleRepository.findRolesByUserId(user.getId());
        if (roleList == null || roleList.isEmpty()) {
            throw new DisabledException("权限异常");
        }

        List<GrantedAuthority> authorityList = new ArrayList<>();
        roleList.forEach(role -> authorityList.add(new SimpleGrantedAuthority("ROLE_" + role.getName())));
        user.setAuthorityList(authorityList);
        return user;
    }

    @Override
    public ServiceResult<UserDto> findById(Long adminId) {
        User user = userRepository.findOne(adminId);
        if (user == null) {
            return ServiceResult.notFound();
        }
        UserDto userDto = modelMapper.map(user, UserDto.class);
        return ServiceResult.of(userDto);
    }
}
