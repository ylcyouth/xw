package com.gagaco.xunxuproj2.service.search;

import com.gagaco.xunxuproj2.service.ServiceMultiResult;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.web.form.RentSearch;

import java.util.List;

/**
 * 搜索服务接口
 * @date 2019-5-2 00:10:54
 * @author wangjiajia
 */
public interface ISearchService {


    void index(Long houseId);

    void remove(Long houseId);

    /**
     * 房源搜索引擎方法
     * @param rentSearch 查询条件
     * @return 搜索到的房源·的ids
     */
    ServiceMultiResult<Long> query(RentSearch rentSearch);


    /**
     * 搜索补全的关键词
     * @param prefix
     * @return
     */
    ServiceResult<List<String>> suggest(String prefix);


    /**
     * 聚合小区的房源数量
     */
    ServiceResult<Long> aggregateDictrictHouse(String cityName, String regionEnName, String district);

    /**
     * 聚合城市房源数据
     */
    ServiceMultiResult<HouseBucketDto> aggregateMap(String cityName);











}
