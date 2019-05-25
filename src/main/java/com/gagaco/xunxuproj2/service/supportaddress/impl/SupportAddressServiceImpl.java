package com.gagaco.xunxuproj2.service.supportaddress.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gagaco.xunxuproj2.entity.Subway;
import com.gagaco.xunxuproj2.entity.SubwayStation;
import com.gagaco.xunxuproj2.entity.SupportAddress;
import com.gagaco.xunxuproj2.repository.SubwayRepository;
import com.gagaco.xunxuproj2.repository.SubwayStationRepository;
import com.gagaco.xunxuproj2.repository.SupportAddressRepository;
import com.gagaco.xunxuproj2.service.ServiceMultiResult;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.service.search.BaiduMapLocation;
import com.gagaco.xunxuproj2.service.supportaddress.ISupportAddressService;
import com.gagaco.xunxuproj2.web.dto.SubwayDto;
import com.gagaco.xunxuproj2.web.dto.SubwayStationDto;
import com.gagaco.xunxuproj2.web.dto.SupportAddressDto;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
    private SupportAddressRepository supportAddressRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    private Logger logger = LoggerFactory.getLogger(SupportAddressServiceImpl.class);

    @Value("${baidu.map.geoconv.api}")
    private String BAIDU_MAP_GEOCONV_API;

    @Value("${baidu.map.server.ak}")
    private String BAIDU_MAP_SERVER_AK;

    @Autowired
    private ObjectMapper objectMapper;

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

    /**
     * 获取城市的详细位置的百度地图经纬度
     *
     * @param city
     * @param address
     */
    @Override
    public ServiceResult<BaiduMapLocation> getBaiduMapLocation(String city, String address) {
        String encodeCity = "";
        String encodeAddress = "";

        //对city和address进行url编码
        try {
            encodeCity = URLEncoder.encode(city, "UTF-8");
            encodeAddress = URLEncoder.encode(address, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("Error to encode house address", e);
            return new ServiceResult<BaiduMapLocation>(false, "Error to encode hosue address");
        }

        //使用HttpClient调用百度地图接口获取经纬度
        //CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpClient httpClient = HttpClients.createDefault();

        StringBuilder sb = new StringBuilder(BAIDU_MAP_GEOCONV_API);
        sb.append("city=").append(encodeCity).append("&");
        sb.append("address=").append(encodeAddress).append("&");
        sb.append("ak=").append(BAIDU_MAP_SERVER_AK).append("&");
        sb.append("output=json");

        HttpGet httpGet = new HttpGet(sb.toString());

        try {
            HttpResponse response = httpClient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                return new ServiceResult<>(false, "Can not get baidu map location");
            }

            String result = EntityUtils.toString(response.getEntity(), "UTF-8");
            JsonNode jsonNode = objectMapper.readTree(result);

            int status = jsonNode.get("status").asInt();

            if (status != 0) {
                return new ServiceResult<>(false, "Error to get map location for status: " + status);
            } else {
                BaiduMapLocation location = new BaiduMapLocation();
                JsonNode jsonLocation = jsonNode.get("result").get("location");
                location.setLongitude(jsonLocation.get("lng").asDouble());
                location.setLatitude(jsonLocation.get("lat").asDouble());
                return ServiceResult.of(location);
            }
        } catch (IOException e) {
            logger.error("Error to fetch baidumap api", e);
            return new ServiceResult<>(false, "Error to fetch baidumap api");
        }
    }




    public static final String BAIDU_MAP_LBS_COORD_TYPE = "3";
    public static final String BAIDU_MAP_LBS_GEOTABLE_ID = "202281";
    public static final String BAIDU_MAP_LBS_CREATE_API = "http://api.map.baidu.com/geodata/v3/poi/create";
    public static final String BAIDU_MAP_LBS_UPDATE_API = "http://api.map.baidu.com/geodata/v3/poi/update";
    public static final String BAIDU_MAP_LBS_QUERY_API = "http://api.map.baidu.com/geodata/v3/poi/list?";


    private static final String LBS_DELETE_API = "http://api.map.baidu.com/geodata/v3/poi/delete";


    /**
     * 上传百度LBS数据
     *
     * @param location
     * @param title
     * @param address
     * @param houseId
     * @param price
     * @param area
     */
    @Override
    public ServiceResult lbsUpload(BaiduMapLocation location, String title, String address, Long houseId, int price, int area) {

        HttpClient httpClient = HttpClients.createDefault();

        //百度上传LBS数据需要的参数
        List<NameValuePair> nvpList = new ArrayList<>();
        nvpList.add(new BasicNameValuePair("ak", BAIDU_MAP_SERVER_AK));

        //百度坐标系
        nvpList.add(new BasicNameValuePair("coord_type", BAIDU_MAP_LBS_COORD_TYPE));

        //虎鲸数据管理平台中数据表的id
        nvpList.add(new BasicNameValuePair("geotable_id", BAIDU_MAP_LBS_GEOTABLE_ID));

        nvpList.add(new BasicNameValuePair("latitude", String.valueOf(location.getLatitude())));
        nvpList.add(new BasicNameValuePair("longitude", String.valueOf(location.getLongitude())));

        nvpList.add(new BasicNameValuePair("houseId", String.valueOf(houseId)));
        nvpList.add(new BasicNameValuePair("price", String.valueOf(price)));
        nvpList.add(new BasicNameValuePair("area", String.valueOf(area)));
        nvpList.add(new BasicNameValuePair("title", String.valueOf(title)));
        nvpList.add(new BasicNameValuePair("address", String.valueOf(address)));

        HttpPost post;
        //判断是搜已经有该house的LBS数据，有调用更新接口，没有创建接口
        if (isLbsDataExists(houseId)) {
            post = new HttpPost(BAIDU_MAP_LBS_UPDATE_API);
        } else {
            post = new HttpPost(BAIDU_MAP_LBS_CREATE_API);
        }

        try {
            post.setEntity(new UrlEncodedFormEntity(nvpList, "UTF-8"));

            HttpResponse response = httpClient.execute(post);

            String respEntity = EntityUtils.toString(response.getEntity(), "UTF-8");

            //判断响应状态码是否为200， 不为200返回false信息， 为200接着判断响应实体的状态
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("Can not upload lbs data for response: " + respEntity);
                return new ServiceResult(false, "Can not upload lbs data");
            }

            JsonNode jsonNode = objectMapper.readTree(respEntity);
            int status = jsonNode.get("status").asInt();
            //响应实体的状态是否为0, 不为0返回false信息，为0返回success信息
            if (0 != status) {
                String message = jsonNode.get("message").asText();
                logger.error("Error to upload lbs data for status: {}, and message: {}", status, message);
                return new ServiceResult(false, "Error to upload lbs data");
            } else  {
                return ServiceResult.success();
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ServiceResult(false);
    }

    /**
     * 根据houseId判断虎鲸数据平台是否已经存在该房源的LBS数据, 上传数据的时候根据此决定是更新数据还是插入数据
     * @param houseId
     * @return 存在返回true, 不存在返回false
     */
    private boolean isLbsDataExists(Long houseId) {

        HttpClient httpClient = HttpClients.createDefault();

        StringBuilder sb = new StringBuilder(BAIDU_MAP_LBS_QUERY_API)
                .append("geotable_id=").append(BAIDU_MAP_LBS_GEOTABLE_ID).append("&")
                .append("ak=").append(BAIDU_MAP_SERVER_AK).append("&")
                .append("houseId=").append(houseId).append(",").append(houseId);

        HttpGet httpGet = new HttpGet(sb.toString());

        try {
            HttpResponse getResp = httpClient.execute(httpGet);
            String respEntity = EntityUtils.toString(getResp.getEntity(), "UTF-8");

            if (getResp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                return false;
            }

            JsonNode jsonNode = objectMapper.readTree(respEntity);
            int status = jsonNode.get("status").asInt();
            if (status != 0) {
                return false;
            }

            int size = jsonNode.get("size").asInt();
            if (size <= 0) {
                return false;
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除百度LBS数据
     *
     * @param houseId
     */
    @Override
    public ServiceResult lbsRemove(Long houseId) {













        return null;
    }

}
