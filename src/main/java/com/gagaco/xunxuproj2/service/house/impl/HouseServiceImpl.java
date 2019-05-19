package com.gagaco.xunxuproj2.service.house.impl;

import com.gagaco.xunxuproj2.base.HouseSort;
import com.gagaco.xunxuproj2.base.HouseStatus;
import com.gagaco.xunxuproj2.base.LoginUtil;
import com.gagaco.xunxuproj2.entity.*;
import com.gagaco.xunxuproj2.repository.*;
import com.gagaco.xunxuproj2.service.ServiceMultiResult;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.service.house.IHouseService;
import com.gagaco.xunxuproj2.service.house.IQiniuService;
import com.gagaco.xunxuproj2.service.search.ISearchService;
import com.gagaco.xunxuproj2.service.search.MapSearch;
import com.gagaco.xunxuproj2.web.dto.HouseDetailDto;
import com.gagaco.xunxuproj2.web.dto.HouseDto;
import com.gagaco.xunxuproj2.web.dto.HousePictureDto;
import com.gagaco.xunxuproj2.web.form.DataTableSearch;
import com.gagaco.xunxuproj2.web.form.HouseForm;
import com.gagaco.xunxuproj2.web.form.RentSearch;
import com.google.common.collect.Maps;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.*;

@Service
public class HouseServiceImpl implements IHouseService {

    @Autowired
    private SubwayRepository subwayRepository;

    @Autowired
    private SubwayStationRepository subwayStationRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private HouseDetailRepository houseDetailRepository;

    @Value("${qiniu.cdn.prefix}")
    private String cdnPrefix;

    @Autowired
    private HousePictureRepository housePictureRepository;

    @Autowired
    private HouseTagRepository houseTagRepository;

    @Autowired
    private HouseSubscribeRespository houseSubscribeRespository;

    @Autowired
    private IQiniuService qiniuService;

    @Autowired
    private ISearchService searchService;

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    @Override
    public ServiceResult<HouseDto> save(HouseForm houseForm) {


        HouseDetail houseDetail = new HouseDetail();
        //填充房源详细信息对象
        ServiceResult subwayValidationResult = wrapperHouseDetailInfo(houseDetail, houseForm);
        if (subwayValidationResult != null) {
            return subwayValidationResult;
        }

        House house = new House();
        //填充房源基本信息
        modelMapper.map(houseForm, house);

        //填充房源其他基本信息
        Date now = new Date();
        house.setCreateTime(now);
        house.setLastUpdateTime(now);
        house.setAdminId(LoginUtil.getLoginUserId());

        //增加房源
        houseRepository.save(house);

        //房源详情信息设置房源id
        houseDetail.setHouseId(house.getId());

        //增加房源详细信息
        houseDetail = houseDetailRepository.save(houseDetail);

        //填充房源图片信息对象
        List<HousePicture> housePictureList = generatorHousePictures(houseForm, house.getId());
        Iterable<HousePicture> housePictures = housePictureRepository.save(housePictureList);

        HouseDto houseDto = modelMapper.map(house, HouseDto.class);
        HouseDetailDto houseDetailDto = modelMapper.map(houseDetail, HouseDetailDto.class);

        houseDto.setHouseDetail(houseDetailDto);

        List<HousePictureDto> housePictureDtoList = new ArrayList<>();

        housePictures.forEach(
                housePicture -> housePictureDtoList.add(modelMapper.map(housePicture, HousePictureDto.class)));
        houseDto.setPictures(housePictureDtoList);
        houseDto.setCover(this.cdnPrefix + houseDto.getCover());

        List<String> tags = houseForm.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<HouseTag> houseTags = new ArrayList<>();
            tags.forEach(tag -> houseTags.add(new HouseTag(house.getId(), tag)));
            houseTagRepository.save(houseTags);
            houseDto.setTags(tags);
        }

        return new ServiceResult<>(true, null, houseDto);
    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    @Override
    public ServiceMultiResult<HouseDto> adminQuery(DataTableSearch searchBody) {
        /*
        //最基础的 直接查出来的整个房源的列表
        List<HouseDto> houseDtos = new ArrayList<>();

        Iterable<House> houses = houseRepository.findAll();

        houses.forEach(house -> {
            HouseDto houseDto = modelMapper.map(house, HouseDto.class);
            houseDto.setCover(this.cdnPrefix + house.getCover());
            houseDtos.add(houseDto);
        });
        return new ServiceMultiResult<>(houseDtos.size(), houseDtos);
        */

        /*
        //单个维度的分页和排序
        List<HouseDto> houseDtos = new ArrayList<>();

        Sort sort = new Sort(Sort.Direction.fromString(searchBody.getDirection()), searchBody.getOrderBy());
        int page = searchBody.getStart()/searchBody.getLength();

        //要实现多维度排序 需要在这里加上 Specification，请看下面的代码

        Pageable pageable = new PageRequest(page, searchBody.getLength(), sort);
        Page<House> houses = houseRepository.findAll(pageable);
        houses.forEach(house -> {
            HouseDto houseDto = modelMapper.map(house, HouseDto.class);
            houseDto.setCover(this.cdnPrefix +  house.getCover());
            houseDtos.add(houseDto);
        });
        return new ServiceMultiResult<>(houses.getTotalElements(), houseDtos);
        */

        //多维度的分页排序
        List<HouseDto> houseDtos = new ArrayList<>();

        Sort sort = new Sort(Sort.Direction.fromString(searchBody.getDirection()), searchBody.getOrderBy());
        int page = searchBody.getStart()/searchBody.getLength();

        Pageable pageable = new PageRequest(page, searchBody.getLength(), sort);

        //为了实现多维度排序加上 Specification
        //Java8的方式实现接口
        Specification<House> specification = (root, query, cb) -> {
            //这2个条件是一定要满足的
            Predicate predicate = cb.equal(root.get("adminId"), LoginUtil.getLoginUserId());
            predicate = cb.and(predicate, cb.notEqual(root.get("status"), HouseStatus.DELETED.getValue()));

            //其他的查询条件 下面分别对应着房源列表的 城市名，房源状态，创建时间，房源标题 这个几个查询条件
            //城市名
            if (searchBody.getCity() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("cityEnName"), searchBody.getCity()));
            }
            //房源状态 这里就解释了 问什么在DataTableSearch类中 status状态 使用Integer包装类型，而不使用int类型
            if (searchBody.getStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), searchBody.getStatus()));
            }
            //创建时间
            if (searchBody.getCreateTimeMin() != null) {
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMin()));
            }
            //创建时间
            if (searchBody.getCreateTimeMax() != null) {
                predicate = cb.and(predicate,
                        cb.lessThanOrEqualTo(root.get("createTime"), searchBody.getCreateTimeMax()));
            }
            //房源标题
            if (searchBody.getTitle() != null) {
                predicate = cb.and(predicate, cb.like(root.get("title"), "%"+searchBody.getTitle()+"%"));
            }
            return predicate;
        };

        Page<House> houses = houseRepository.findAll(specification, pageable);
        houses.forEach(house -> {
            HouseDto houseDto = modelMapper.map(house, HouseDto.class);
            houseDto.setCover(this.cdnPrefix +  house.getCover());
            houseDtos.add(houseDto);
        });
        return new ServiceMultiResult<>(houses.getTotalElements(), houseDtos);

    }

    @Override
    public ServiceResult<HouseDto> findCompleteOne(Long id) {
        // id 在 controller 里面已经判断了

        //根据 id 查询 house，并做判断
        House house = houseRepository.findOne(id);
        if (house == null) {
            return ServiceResult.notFound();
        }


        //根据 houseId 查询 houseDetail，因为 house 存在，所以不用判断了
        HouseDetail houseDetail = houseDetailRepository.findByHouseId(id);

        //根据 houseId 查询所有的 housePicture，因为 house 存在，所以不用判断了
        List<HousePicture> housePictureList = housePictureRepository.findAllByHouseId(id);

        //根据 houseId 查询所有的 houseTag，因为 house 存在，所以不用判断了
        List<HouseTag> houseTagList= houseTagRepository.findAllByHouseId(id);


        //把 houseDetail 映射成 houseDetailDto
        HouseDetailDto houseDetailDto = modelMapper.map(houseDetail, HouseDetailDto.class);

        //把所有的 housePicture 映射成 housePictureDto
        List<HousePictureDto> housePictureDtoList = new ArrayList<>();
        housePictureList.forEach(housePicture -> {
            HousePictureDto housePictureDto = modelMapper.map(housePicture, HousePictureDto.class);
            housePictureDtoList.add(housePictureDto);
        });

        //取出所有的 houseTag 对象中的 tagName
        List<String> tagNameList = new ArrayList<>();
        houseTagList.forEach(houseTag -> tagNameList.add(houseTag.getName()));


        //把 house 映射成 houseDto，并设置 houseDetail, housePicture, tags
        HouseDto houseDto = modelMapper.map(house, HouseDto.class);
        houseDto.setHouseDetail(houseDetailDto);
        houseDto.setPictures(housePictureDtoList);
        houseDto.setTags(tagNameList);


        // 有疑问？ 预约看房相关
        if (LoginUtil.getLoginUserId() > 0) {
            HouseSubscribe houseSubscribe =
                    houseSubscribeRespository.findByHouseIdAndUserId(house.getId(), LoginUtil.getLoginUserId());
            if (houseSubscribe != null) {
                houseDto.setSubscribeStatus(houseSubscribe.getStatus());
            }
        }

        //到这，houseDto 就封装好了，再用它封装统一的服务返回对象并返回，服务完成
        return ServiceResult.of(houseDto);
    }

    /**
     * 什么操作需要判断操作后的结果，什么操作不需要？
     *
     */
    @Override
    @Transactional
    public ServiceResult update(HouseForm houseForm) {
        //houseForm中的数据已经在controller里通过校验，这里就可以直接使用了

        //根据houseForm中的houseId查询房源
        House house = houseRepository.findOne(houseForm.getId());
        //判断查询的结果 是否为null
        if (house == null) {
            return ServiceResult.notFound();
        }

        //根据查寻到的house的id查询houseDetail
        HouseDetail houseDetail = houseDetailRepository.findByHouseId(house.getId());
        //判断查询结果 是否为null
        if (houseDetail == null) {
            return ServiceResult.notFound();
        }

        //把参数houseForm中的房源详细信息填充进查询到的houseDetail中
        ServiceResult wrapperServiceResult = wrapperHouseDetailInfo(houseDetail, houseForm);
        //判断填充结果 null是填充正常，非null是填充失败
        if (wrapperServiceResult != null) {
            return wrapperServiceResult;
        }

        //保存房源详情 注意这里是 jpa的save方法
        houseDetailRepository.save(houseDetail);

        //填充房源图片信息，每个housePicture都是houseId和picture url关联
        List<HousePicture> housePictureList = generatorHousePictures(houseForm, houseForm.getId());
        //这里返回的的是图片的一个集合，没有判断，直接用了
        //保存房源图片
        housePictureRepository.save(housePictureList);

        //如果houseForm中没有房源封面，就设成原来的封面
        if (houseForm.getCover() == null) {
            houseForm.setCover(house.getCover());
        }

        //把houseForm映射成house, 有变化的新的房源信息覆盖掉原来的house中的信息
        modelMapper.map(houseForm, house);
        //设置house的修改时间
        house.setLastUpdateTime(new Date());

        //保存house
        houseRepository.save(house);

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        /*wjj 2019-5-12 17:27:55*/
        if (house.getStatus() == HouseStatus.PASSES.getValue()) {
            searchService.index(house.getId());
        }
        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~


        return ServiceResult.success();
    }
    /**
     * 删除的时候可以直接根据条件去删除
     * 但是删除之前肯定是要先查出来的，用查出来的对象的id去删更好
     *
     */
    @Override
    public ServiceResult removeTag(Long houseId, String tag) {
        //服务层这里不用判断houseId和tag这两个参数了，控制层已经判断过

        //首先根据houseId查出来house
        House house = houseRepository.findOne(houseId);
        //判断查询house的结果
        if (house == null) {
            return ServiceResult.notFound();
        }

        //接着根据houseId和tag查标签
        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        //判断查询houseTag的结果
        if (houseTag == null) {
            return new ServiceResult(false, "标签不存在");
        }

        //调用数据层删除houseTag的接口
        houseTagRepository.delete(houseTag.getId());
        return ServiceResult.success();
    }

    @Override
    public ServiceResult addTag(Long houseId, String tag) {
        //服务层这里不用判断houseId和tag这两个参数了，控制层已经判断过

        //首先根据houseId查出来house
        House house = houseRepository.findOne(houseId);
        //判断查询house的结果
        if (house == null) {
            return ServiceResult.notFound();
        }

        //接着根据houseId和tag查标签
        HouseTag houseTag = houseTagRepository.findByNameAndHouseId(tag, houseId);
        //判断查询houseTag的结果
        if (houseTag != null) {
            return new ServiceResult(false, "标签已存在");
        }

        //到这儿就可以保存houseTag了
        houseTagRepository.save(new HouseTag(houseId, tag));
        return ServiceResult.success();
    }

    /**
     * 服务层的统一返回对象ServiceResult, 还可以添加一个 服务调用失败构造方法notSuccess，参数是错误信息
     *
     */
    @Override
    public ServiceResult removePhoto(Long id) {

        //首先 根据房源图片的id 调用数据层查询图片的接口
        HousePicture housePicture = housePictureRepository.findOne(id);
        //判断查询结果
        if (housePicture == null) {
            ServiceResult.notFound();
        }

        //删除房源图片
        try {
            //根据查询到的图片path 调用服务层七牛服务删除图片的服务
            Response response = qiniuService.deleteFile(housePicture.getPath());
            //判断服务调用结果，执行对应操作
            if (response.isOK()) {
                //调用数据层删除房源图片的接口删除房源图片
                housePictureRepository.delete(id);
                return ServiceResult.success();
            } else {
                return new ServiceResult(false, response.error);
            }
        } catch (QiniuException e) {
            e.printStackTrace();
            return new ServiceResult(false, e.getMessage());
        }
    }

    /**
     * 这个服务方法改动了数据，必须贴上@Transactional注解，不然Jpa不会让你修改的
     * 上面那个update方法，里面调用的都是save方法，也要贴上注解
     * 但是addTag removeTag removePhoto 也是修改了数据的为什么不需要@Transactional注解呢？？？ 不知道
     *
     */
    @Override
    @Transactional
    public ServiceResult updateCover(Long houseId, Long coverId) {
        //连续写了好几个类似的服务，这个不写注释了

        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        HousePicture cover = housePictureRepository.findOne(coverId);
        if (cover == null) {
            return ServiceResult.notFound();
        }

        houseRepository.updateCover(houseId, cover.getPath());
        return ServiceResult.success();
    }

    @Override
    @Transactional
    public ServiceResult updateStatus(Long houseId, int status) {

        House house = houseRepository.findOne(houseId);
        if (house == null) {
            return ServiceResult.notFound();
        }

        if (house.getStatus() == status) {
            return new ServiceResult(false, "状态没有发生变化");
        }
        if (house.getStatus() == HouseStatus.RENTED.getValue()) {
            return new ServiceResult(false, "房源已经出租, 不允许修改状态");
        }
        if (house.getStatus() == HouseStatus.DELETED.getValue()) {
            return new ServiceResult(false, "房源已经删除，不允许操作");
        }

        houseRepository.updateStatus(houseId, status);

        //~~~~~~~~~~~~~~~~~~~~~
        //索引操作
        /*wjj 2019-5-12 17:29:16*/
        if (status == HouseStatus.PASSES.getValue()) {
            searchService.index(houseId);
        } else {
            searchService.remove(houseId);
        }
        //~~~~~~~~~~~~~~~~~~~~~


        return ServiceResult.success();
    }

    /**
     * 把houseIds封装成houses
     * @param houseIds
     * @return
     */
    private List wrapperHouseResult(List<Long> houseIds) {
        List<HouseDto> result = new ArrayList<>();

        Map<Long, HouseDto> idToHouseDtoMap = new HashMap<>();

        Iterable<House> houses = houseRepository.findAll(houseIds);
        houses.forEach(house -> {
            HouseDto houseDto = modelMapper.map(house, HouseDto.class);
            houseDto.setCover(this.cdnPrefix + house.getCover());
            idToHouseDtoMap.put(house.getId(), houseDto);
        });

        //包装house的详细信息
        this.wrapperHouseList(houseIds, idToHouseDtoMap);

        houseIds.forEach(houseId -> result.add(idToHouseDtoMap.get(houseId)));

        return result;
    }

    @Override
    public ServiceMultiResult query(RentSearch rentSearch) {

        //判断搜索条件中是否有关键词
        if (rentSearch.getKeywords() != null && !rentSearch.getKeywords().isEmpty()) {
            ServiceMultiResult<Long> smr = searchService.query(rentSearch);
            if (smr.getTotal() == 0) {
                return new ServiceMultiResult(0, new ArrayList());
            }
            return new ServiceMultiResult(smr.getTotal(), this.wrapperHouseResult(smr.getResult()));

        }

        return this.simpleQuery(rentSearch);
    }

    @Override
    public ServiceMultiResult<HouseDto> wholeMapQuery(MapSearch mapSearch) {
        ServiceMultiResult<Long> smr = searchService.mapQuery(
                mapSearch.getCityEnName(),
                mapSearch.getOrderBy(),
                mapSearch.getOrderDirection(),
                mapSearch.getStart(),
                mapSearch.getSize()
        );

        //如果这个城市下面没有房源
        if (smr.getTotal() == 0) {
            return new ServiceMultiResult<>(0, new ArrayList<>());
        }

        List<HouseDto> houseList = this.wrapperHouseResult(smr.getResult());
        return new ServiceMultiResult<>(smr.getTotal(), houseList);
    }

    private ServiceMultiResult<HouseDto> simpleQuery(RentSearch rentSearch) {
        //以下是查出所有 发布的房源

        //Sort sort = new Sort(Sort.Direction.DESC, "lastUpdateTime");
        Sort sort = HouseSort.generatorSort(rentSearch.getOrderBy(), rentSearch.getOrderDirection());


        //有疑问
        int page = rentSearch.getStart() / rentSearch.getSize();

        Pageable pageable = new PageRequest(page, rentSearch.getSize(), sort);

        Specification<House> specification = (root, criteriaQuery, criteriaBuilder) -> {

            //这里还要加上处理 价格区间 还有 面积区间的 代码，现在前台房源信息浏览页面是无法无法根据这个条件搜索的

            Predicate predicate = criteriaBuilder.equal(root.get("status"), HouseStatus.PASSES.getValue());
            predicate = criteriaBuilder
                    .and(predicate, criteriaBuilder.equal(root.get("cityEnName"), rentSearch.getCityEnName()));

            //当根据距地铁站的距离排序时，处理没有地铁站的情况
            if (HouseSort.DISTANCE_TO_SUBWAY_KEY.equals(rentSearch.getOrderBy())) {
                predicate = criteriaBuilder
                        .and(predicate, criteriaBuilder.gt(root.get(HouseSort.DISTANCE_TO_SUBWAY_KEY), -1));
            }
            return predicate;
        };

        Page<House> houses = houseRepository.findAll(specification, pageable);

        List<Long> houseIds = new ArrayList<>();
        List<HouseDto> houseDtos = new ArrayList<>();
        Map<Long, HouseDto> idToHouseMap = Maps.newHashMap();

        houses.forEach(house -> {
            HouseDto houseDto = modelMapper.map(house, HouseDto.class);
            houseDto.setCover(cdnPrefix + house.getCover());
            houseDtos.add(houseDto);

            houseIds.add(house.getId());
            idToHouseMap.put(house.getId(), houseDto);
        });

        wrapperHouseList(houseIds, idToHouseMap);

        return new ServiceMultiResult(houses.getTotalElements(), houseDtos);
    }


    /**
     * 前台房源浏览页面用
     * 房源详细信息
     *
     */
    private void wrapperHouseList(List<Long> houseIds, Map<Long,HouseDto> idToHouseMap) {

        //这个方法Jpa怎么实现的？
        List<HouseDetail> houseDetails = houseDetailRepository.findAllByHouseIdIn(houseIds);

        houseDetails.forEach(houseDetail -> {
            HouseDto houseDto = idToHouseMap.get(houseDetail.getHouseId());
            HouseDetailDto houseDetailDto = modelMapper.map(houseDetail, HouseDetailDto.class);
            houseDto.setHouseDetail(houseDetailDto);
        });

        List<HouseTag> houseTags = houseTagRepository.findAllByHouseIdIn(houseIds);
        houseTags.forEach(houseTag -> {
            HouseDto houseDto = idToHouseMap.get(houseTag.getHouseId());
            houseDto.getTags().add(houseTag.getName());
        });
    }

   /* @Override
    public ServiceMultiResult query(RentSearch rentSearch) {

        if (rentSearch.getKeyword() != null && !rentSearch.getKeyword().isEmpty()) {
            ServiceMultiResult<Long> serviceMultiResult = searchService.query(rentSearch);
            if (serviceMultiResult.getTotal() == 0) {
                return new ServiceMultiResult(0, new ArrayList<>());
            }
            return new ServiceMultiResult(serviceMultiResult.getTotal(),
                    wrapperHouseResult(serviceMultiResult.getResult()));
        }
        return simpleQuery(rentSearch);
    }*/



   /* private List wrapperHouseResult(List<Long> result) {

    }


    private ServiceMultiResult simpleQuery(RentSearch rentSearch) {
    }*/


    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    //这一块是私有方法

    /**
     * 填充房源详细信息对象
     */
    private ServiceResult wrapperHouseDetailInfo(HouseDetail houseDetail, HouseForm houseForm) {


        //根据地铁线id查不到地铁线
        Subway subway = subwayRepository.findOne(houseForm.getSubwayLineId());
        if (subway == null) {
            return new ServiceResult<>(false, "Not Valid Subway Line!");
        }

        //根据地铁站id查不到地铁站
        SubwayStation subwayStation = subwayStationRepository.findOne(houseForm.getSubwayStationId());
        if (subwayStation == null || !subway.getId().equals(subwayStation.getSubwayId()) ) {
            return new ServiceResult<>(false, "Not Valid Subway Station");
        }

        //houseDetail设置地铁线
        houseDetail.setSubwayLineId(subway.getId());
        houseDetail.setSubwayLineName(subway.getName());

        //houseDetail设置地铁站
        houseDetail.setSubwayStationId(subwayStation.getId());
        houseDetail.setSubwayStationName(subwayStation.getName());

        //houseDetail设置房源其他信息
        houseDetail.setDescription(houseForm.getDescription());
        houseDetail.setDetailAddress(houseForm.getDetailAddress());
        houseDetail.setLayoutDesc(houseForm.getLayoutDesc());
        houseDetail.setRentWay(houseForm.getRentWay());
        houseDetail.setRoundService(houseForm.getRoundService());
        houseDetail.setTraffic(houseForm.getTraffic());
        return null;
    }

    /**
     * 填充房源图片信息对象
     *
     */
    private List<HousePicture> generatorHousePictures(HouseForm houseForm, Long houseId) {
        List<HousePicture> housePictureList = new ArrayList<>();
        //如果没有上传房源图片
        if (houseForm.getPhotos() == null || houseForm.getPhotos().isEmpty()) {
            return housePictureList;
        }

        //photoForm -> housePicture,把photoFormList里的信息填充到 housePictureList中
        houseForm.getPhotos().forEach(photoForm -> {
            HousePicture picture = new HousePicture();
            picture.setHouseId(houseId);
            picture.setCdnPrefix(cdnPrefix);
            picture.setPath(photoForm.getPath());
            picture.setWidth(photoForm.getWidth());
            picture.setHeight(photoForm.getHeight());
            housePictureList.add(picture);
        });
        return housePictureList;
    }

}
