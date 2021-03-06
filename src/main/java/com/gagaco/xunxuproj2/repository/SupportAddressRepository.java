package com.gagaco.xunxuproj2.repository;

import com.gagaco.xunxuproj2.entity.SupportAddress;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @time 2019-4-24 22:43:19
 * @author wangjiajia
 *
 *
 */
@Repository
public interface SupportAddressRepository extends CrudRepository<SupportAddress, Long> {


    List<SupportAddress> findAllByLevel(String level);


    List<SupportAddress> findAllByLevelAndBelongTo(String level, String belongTo);

    SupportAddress findByEnNameAndLevel(String enName, String level);

    SupportAddress findByEnNameAndBelongTo(String enName, String belongTo);
}
