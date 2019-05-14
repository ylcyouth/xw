package com.gagaco.xunxuproj2.repository;

import com.gagaco.xunxuproj2.entity.House;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Created by 瓦力.
 *
 * PagingAndSortingRepository 接口 是继承了 CrudRepository的， 并在自身添加了 分页和排序相关的findAll方法
 * 在这里因为要用到分页和排序所以直接继承了 PagingAndSortingRepository方法
 *
 * 后来为了实现多维度的分页和排序 有 继承了 JpaSpecificationExecutor接口
 *
 */
@Repository
public interface HouseRepository extends PagingAndSortingRepository<House, Long>, JpaSpecificationExecutor<House> {
    @Modifying
    @Query("update House as house set house.cover = :cover where house.id = :id")
    void updateCover(@Param(value = "id") Long id, @Param(value = "cover") String cover);

    @Modifying
    @Query("update House as house set house.status = :status where house.id = :id")
    void updateStatus(@Param(value = "id") Long id, @Param(value = "status") int status);

    @Modifying
    @Query("update House as house set house.watchTimes = house.watchTimes + 1 where house.id = :id")
    void updateWatchTimes(@Param(value = "id") Long houseId);
}
