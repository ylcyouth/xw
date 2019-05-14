package com.gagaco.xunxuproj2.repository;

import com.gagaco.xunxuproj2.entity.Subway;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by 瓦力.
 */
@Repository
public interface SubwayRepository extends CrudRepository<Subway, Long>{
    List<Subway> findAllByCityEnName(String cityEnName);
}
