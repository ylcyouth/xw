package com.gagaco.xunxuproj2.repository;

import com.gagaco.xunxuproj2.entity.Role;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * @time 2019-4-21 19:55:29
 * @author wangjiajia
 */
public interface RoleRepository extends CrudRepository<Role, Long> {

    List<Role> findRolesByUserId(Long userId);

}
