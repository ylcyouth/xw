package com.gagaco.xunxuproj2.service.house;

import com.gagaco.xunxuproj2.service.ServiceMultiResult;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.service.search.MapSearch;
import com.gagaco.xunxuproj2.web.dto.HouseDto;
import com.gagaco.xunxuproj2.web.form.DataTableSearch;
import com.gagaco.xunxuproj2.web.form.HouseForm;
import com.gagaco.xunxuproj2.web.form.RentSearch;

/**
 * @time 2019-4-25 23:15:10
 * @author wangjiajia
 *
 */
public interface IHouseService {

    /**
     * 增加 house
     *
     */
    ServiceResult<HouseDto> save(HouseForm houseForm);

    ServiceMultiResult<HouseDto> adminQuery(DataTableSearch searchBody);

    ServiceResult<HouseDto> findCompleteOne(Long id);

    ServiceResult update(HouseForm houseForm);

    ServiceResult addTag(Long houseId, String tag);

    ServiceResult removeTag(Long houseId, String tag);

    ServiceResult removePhoto(Long id);

    ServiceResult updateCover(Long houseId, Long coverId);

    ServiceResult updateStatus(Long houseId, int status);

    ServiceMultiResult query(RentSearch rentSearch);

    ServiceMultiResult<HouseDto> wholeMapQuery(MapSearch mapSearch);

    /**
     * 地图精确范围的数据查询
     */
    ServiceMultiResult<HouseDto> boundMapQuery(MapSearch mapSearch);
}
