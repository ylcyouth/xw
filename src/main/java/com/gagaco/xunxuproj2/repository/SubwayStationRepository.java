package com.gagaco.xunxuproj2.repository;

import com.gagaco.xunxuproj2.entity.SubwayStation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by 瓦力.
 */
@Repository
public interface SubwayStationRepository extends CrudRepository<SubwayStation, Long> {
    List<SubwayStation> findAllBySubwayId(Long subwayId);

}
