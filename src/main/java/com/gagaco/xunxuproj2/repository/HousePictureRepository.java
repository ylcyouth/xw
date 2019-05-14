package com.gagaco.xunxuproj2.repository;

import com.gagaco.xunxuproj2.entity.HousePicture;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by 瓦力.
 */
@Repository
public interface HousePictureRepository extends CrudRepository<HousePicture, Long> {
    List<HousePicture> findAllByHouseId(Long id);
}
