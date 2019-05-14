package com.gagaco.xunxuproj2.service.supportaddress;

import com.gagaco.xunxuproj2.entity.SupportAddress;
import com.gagaco.xunxuproj2.service.ServiceMultiResult;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.web.dto.SubwayDto;
import com.gagaco.xunxuproj2.web.dto.SubwayStationDto;
import com.gagaco.xunxuproj2.web.dto.SupportAddressDto;

import java.util.List;
import java.util.Map;

/**
 * @time 2019-4-24 22:45:49
 * @author wangjiajia
 *
 *
 */
public interface ISupportAddressService {

    /**
     * 获取所有支持的城市列表
     *
     */
    ServiceMultiResult<SupportAddressDto> findAllCities();

    /**
     * 根据城市英文简写获取该城市所有支持的区域信息
     *
     */
    ServiceMultiResult<SupportAddressDto> findAllRegionsByCityName(String cityEnName);

    /**
     * 获取该城市所有的地铁线路
     *
     */
    List<SubwayDto> findAllSubwayByCity(String cityEnName);

    /**
     *
     *
     */
    List<SubwayStationDto> findAllStationBySubway(Long subwayId);

    /**
     *
     *
     */
    Map<SupportAddress.Level,SupportAddressDto> findCityAndRegion(String cityName, String regionEnName);

    ServiceResult<SubwayDto> findSubway(Long subwayLineId);

    ServiceResult<SubwayStationDto> findSubwayStation(Long subwayStationId);

    ServiceResult<SupportAddressDto> findCity(String cityEnName);
}
