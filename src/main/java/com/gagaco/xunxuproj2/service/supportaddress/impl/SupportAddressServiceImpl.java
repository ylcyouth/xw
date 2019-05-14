package com.gagaco.xunxuproj2.service.supportaddress.impl;

import com.gagaco.xunxuproj2.entity.Subway;
import com.gagaco.xunxuproj2.entity.SubwayStation;
import com.gagaco.xunxuproj2.entity.SupportAddress;
import com.gagaco.xunxuproj2.repository.SubwayRepository;
import com.gagaco.xunxuproj2.repository.SubwayStationRepository;
import com.gagaco.xunxuproj2.repository.SupportAddreeeRepository;
import com.gagaco.xunxuproj2.service.ServiceMultiResult;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.service.supportaddress.ISupportAddressService;
import com.gagaco.xunxuproj2.web.dto.SubwayDto;
import com.gagaco.xunxuproj2.web.dto.SubwayStationDto;
import com.gagaco.xunxuproj2.web.dto.SupportAddressDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.processing.SupportedOptions;
import javax.swing.text.AbstractDocument;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @time 2019-4-24 22:47:40
 * @author wangjiajia
 *
 *
 *
 */
@Service
public class SupportAddressServiceImpl implements ISupportAddressService {

    @Autowired
    private SupportAddreeeRepository supportAddressRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Override
    public ServiceMultiResult<SupportAddressDto> findAllCities() {
        List<SupportAddress> addresses = supportAddressRepository.findAllByLevel(SupportAddress.Level.CITY.getValue());
        List<SupportAddressDto> addressDtos = new ArrayList<>();
        for (SupportAddress supportAddress : addresses) {
            SupportAddressDto target = modelMapper.map(supportAddress, SupportAddressDto.class);
            addressDtos.add(target);
        }
        return new ServiceMultiResult<>(addressDtos.size(), addressDtos);
    }

    @Override
    public ServiceMultiResult<SupportAddressDto> findAllRegionsByCityName(String cityName) {
        if (cityName == null) {
            return new ServiceMultiResult<>(0, null);
        }
        List<SupportAddress> regions = supportAddressRepository.findAllByLevelAndBelongTo(
                SupportAddress.Level.REGION.getValue(), cityName);
        List<SupportAddressDto> result = new ArrayList<>();
        for (SupportAddress region : regions) {
            result.add(modelMapper.map(region, SupportAddressDto.class));
        }
        return new ServiceMultiResult<>(regions.size(), result);
    }

    @Override
    public List<SubwayDto> findAllSubwayByCity(String cityEnName) {
        List<SubwayDto> result = new ArrayList<>();
        List<Subway> subways = subwayRepository.findAllByCityEnName(cityEnName);
        if (subways.isEmpty()) {
            return result;
        }
        subways.forEach(subway -> result.add(modelMapper.map(subway, SubwayDto.class)));
        return result;
    }

    @Override
    public List<SubwayStationDto> findAllStationBySubway(Long subwayId) {
        List<SubwayStationDto> result = new ArrayList<>();
        List<SubwayStation> stations = subwayStationRepository.findAllBySubwayId(subwayId);
        if (stations.isEmpty()) {
            return result;
        }

        stations.forEach(station -> result.add(modelMapper.map(station, SubwayStationDto.class)));
        return result;
    }

    @Override
    public Map<SupportAddress.Level, SupportAddressDto> findCityAndRegion(String cityName, String regionEnName) {

        Map<SupportAddress.Level, SupportAddressDto> result = new HashMap<>();

        SupportAddress city = supportAddressRepository.findByEnNameAndLevel(cityName, SupportAddress.Level.CITY.getValue());
        SupportAddress region = supportAddressRepository.findByEnNameAndBelongTo(regionEnName, city.getEnName());

        result.put(SupportAddress.Level.CITY, modelMapper.map(city, SupportAddressDto.class));
        result.put(SupportAddress.Level.REGION, modelMapper.map(region, SupportAddressDto.class));

        return result;
    }

    @Override
    public ServiceResult<SubwayDto> findSubway(Long subwayLineId) {
        // controller 里直接传的参，所以判断参数
        if (subwayLineId == null) {
            return ServiceResult.notFound();
        }

        //根据 subwayLineId 查询 subway，并判断
        Subway subway = subwayRepository.findOne(subwayLineId);
        if (subway == null) {
            return ServiceResult.notFound();
        }

        //到这，把 subway 映射成 subwayDto，并用它封装统一的服务返回对象，返回，服务完成
        return ServiceResult.of(modelMapper.map(subway, SubwayDto.class));
    }

    @Override
    public ServiceResult<SubwayStationDto> findSubwayStation(Long subwayStationId) {
        // controller 里面直接传的参，所以判断参数
        if (subwayStationId == null) {
            return ServiceResult.notFound();
        }

        // 根据 subwayStationId 查询 subwayStationId，并判断
        SubwayStation subwayStation = subwayStationRepository.findOne(subwayStationId);
        if (subwayStation == null) {
            return ServiceResult.notFound();
        }

        //到这，把 subwayStation 映射成 subwayStationDto，并用它封装统一的服务返回对象，返回，服务完成
        return ServiceResult.of(modelMapper.map(subwayStation, SubwayStationDto.class));
    }

    @Override
    public ServiceResult<SupportAddressDto> findCity(String cityEnName) {

        SupportAddress currentCity =
                supportAddressRepository.findByEnNameAndLevel(cityEnName, SupportAddress.Level.CITY.getValue());
        if (currentCity == null) {
            return ServiceResult.notFound();
        }

        SupportAddressDto currentCityDto = modelMapper.map(currentCity, SupportAddressDto.class);

        return ServiceResult.of(currentCityDto);
    }



























}
