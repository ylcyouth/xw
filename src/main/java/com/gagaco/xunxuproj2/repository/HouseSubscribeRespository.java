package com.gagaco.xunxuproj2.repository;

import com.gagaco.xunxuproj2.entity.HouseSubscribe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Created by 瓦力.
 */
@Repository
public interface HouseSubscribeRespository extends PagingAndSortingRepository<HouseSubscribe, Long>{

    HouseSubscribe findByHouseIdAndUserId(Long houseId, Long loginUserId);

    Page<HouseSubscribe> findAllByUserIdAndStatus(Long userId, int status, Pageable pageable);

    Page<HouseSubscribe> findAllByAdminIdAndStatus(Long adminId, int status, Pageable pageable);

    HouseSubscribe findByHouseIdAndAdminId(Long houseId, Long adminId);

    @Modifying
    @Query("update HouseSubscribe as subscribe set subscribe.status = :status where subscribe.id = :id")
    void updateStatus(@Param(value = "id") Long id, @Param(value = "status") int status);
}
