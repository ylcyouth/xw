package com.gagaco.xunxuproj2.repository;

import com.gagaco.xunxuproj2.entity.User;
import org.springframework.data.repository.CrudRepository;

/**
 * @author wangjiajia
 */
public interface UserRepository extends CrudRepository<User, Long> {

    User findByName(String username);
}
