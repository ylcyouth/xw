package com.gagaco.xunxuproj2.web.controller.house;

import com.gagaco.xunxuproj2.base.ApiResponse;
import com.gagaco.xunxuproj2.base.RentValueBlock;
import com.gagaco.xunxuproj2.entity.SupportAddress;
import com.gagaco.xunxuproj2.service.ServiceMultiResult;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.service.house.IHouseService;
import com.gagaco.xunxuproj2.service.search.ISearchService;
import com.gagaco.xunxuproj2.service.supportaddress.ISupportAddressService;
import com.gagaco.xunxuproj2.service.user.IUserService;
import com.gagaco.xunxuproj2.web.dto.*;
import com.gagaco.xunxuproj2.web.form.RentSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * @author wangjiajia
 * @time 2019-4-24 22:31:32
 */
@Controller
public class HouseController {

    @Autowired
    private ISupportAddressService supportAddressService;

    @Autowired
    private IHouseService houseService;

    @Autowired
    private IUserService userService;

    @Autowired
    private ISearchService searchService;

    /**
     * search as you type
     * 自动补全接口
     */
    @GetMapping("rent/house/autocomplete")
    @ResponseBody
    public ApiResponse autoComplete(String prefix) {
        if (prefix.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }
        ServiceResult<List<String>> result = searchService.suggest(prefix);
        return ApiResponse.ofSuccess(result.getResult());
    }


    /**
     * 获取所有支持的城市列表
     */
    @GetMapping("/address/support/cities")
    @ResponseBody
    public ApiResponse getSupportCities() {
        ServiceMultiResult<SupportAddressDto> result = supportAddressService.findAllCities();
        /*要不要考虑为null, 可不可能为null*/
        if (result.getResultSize() == 0) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(result.getResult());
    }

    /**
     * 获取城市对应的支持的区域
     */
    @GetMapping("address/support/regions")
    @ResponseBody
    public ApiResponse getSupportRegions(@RequestParam(name = "city_name") String cityEnName) {
        ServiceMultiResult<SupportAddressDto> result = supportAddressService.findAllRegionsByCityName(cityEnName);
        if (result.getResult() == null || result.getTotal() < 1) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(result.getResult());
    }

    /**
     * 获取城市对应的支持的地铁线路
     */
    @GetMapping("address/support/subway/line")
    @ResponseBody
    public ApiResponse getSupportSubwayLine(@RequestParam(name = "city_name") String cityEnName) {
        List<SubwayDto> subways = supportAddressService.findAllSubwayByCity(cityEnName);
        if (subways.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(subways);
    }

    /**
     * 获取对应地铁线路所支持的地铁站点
     */
    @GetMapping("address/support/subway/station")
    @ResponseBody
    public ApiResponse getSupportSubwayStation(@RequestParam(name = "subway_id") Long subwayId) {
        List<SubwayStationDto> stationDtoS = supportAddressService.findAllStationBySubway(subwayId);
        if (stationDtoS.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_FOUND);
        }
        return ApiResponse.ofSuccess(stationDtoS);
    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    /**
     * 前台房源信息浏览页面
     */
    @GetMapping("rent/house")
    public String rentHousePage(@ModelAttribute RentSearch rentSearch,
                                Model model,
                                HttpSession httpSession,
                                RedirectAttributes redirectAttributes) {


        /*

         */
        //首先判断城市
        String cityEnNameInRentSearch = rentSearch.getCityEnName();
        if (cityEnNameInRentSearch == null) {
            String cityEnNameInSession = (String) httpSession.getAttribute("cityEnName");
            //判断从session中获取的cityEnName
            if (cityEnNameInSession == null) {
                redirectAttributes.addAttribute("msg", "must_chose_city");
                return "redirect:/index";
            } else {
                rentSearch.setCityEnName(cityEnNameInSession);
            }
        } else {
            httpSession.setAttribute("cityEnName", cityEnNameInRentSearch);
        }

        //调用address服务根据cityEnName查询到对应的cityDto
        ServiceResult<SupportAddressDto> currentCityServiceResult =
                supportAddressService.findCity(cityEnNameInRentSearch);
        //判断服务调用结果
        if (!currentCityServiceResult.isSuccess()) {
            redirectAttributes.addAttribute("msg", "must_chose_city");
            return "redirect:/index";
        }

        //把city塞进model中
        model.addAttribute("currentCity", currentCityServiceResult.getResult());

        //调用address服务根据cityEnName查询到对应的所有区域
        ServiceMultiResult<SupportAddressDto> allRegions =
                supportAddressService.findAllRegionsByCityName(cityEnNameInRentSearch);
        //判断服务调用结果
        if (allRegions.getResult() == null || allRegions.getTotal() < 1) {
            redirectAttributes.addAttribute("msg", "must_chose_city");
            return "redirect:/index";
        }




        //调用house搜索服务根据rentSearch搜索条件查询所有的house
        ServiceMultiResult queryResult = houseService.query(rentSearch);
        //这里不用判断服务调用结果了

        //把查到的所有房源塞进mode中，把查到的房源的数量塞进model中
        model.addAttribute("total", queryResult.getTotal());
        model.addAttribute("houses", queryResult.getResult());
        //测试用
//        model.addAttribute("houses", new ArrayList<>());
//        model.addAttribute("total", 0);





        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

        //判断rentSearch中的regionName
        if (rentSearch.getRegionEnName() == null) {
            rentSearch.setRegionEnName("*");
        }

        //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        //塞其他的model属性
        model.addAttribute("searchBody", rentSearch);
        model.addAttribute("regions", allRegions.getResult());
        model.addAttribute("priceBlocks", RentValueBlock.PRICE_BLOCK);
        model.addAttribute("areaBlocks", RentValueBlock.AREA_BLOCK);
        model.addAttribute("currentPriceBlock", RentValueBlock.matchPrice(rentSearch.getPriceBlock()));
        model.addAttribute("currentAreaBlock", RentValueBlock.matchArea(rentSearch.getAreaBlock()));

        return "rent-list";
    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    /**
     * 房源详情页
     *
     */
    @GetMapping("rent/house/show/{id}")
    public String show(@PathVariable(value = "id") Long houseId, Model model) {

        if (houseId < 1) {
            return "404";
        }

        //调用houseService服务根据houseId查询完整的房源
        ServiceResult<HouseDto> completeOneSr = houseService.findCompleteOne(houseId);
        if (!completeOneSr.isSuccess()) {
            return "404";
        }

        //调用supportAddressService根据查询到的完整房源中的cityEnName和regionEnName查询city和region
        HouseDto houseDto = completeOneSr.getResult();
        Map<SupportAddress.Level, SupportAddressDto> cityAndRegionMap =
                supportAddressService.findCityAndRegion(houseDto.getCityEnName(), houseDto.getRegionEnName());

        //把查询到的city和region放到model中
        model.addAttribute("city", cityAndRegionMap.get(SupportAddress.Level.CITY));
        model.addAttribute("region", cityAndRegionMap.get(SupportAddress.Level.REGION));


        ServiceResult<UserDto> userDtoSr = userService.findById(houseDto.getAdminId());
        model.addAttribute("agent", userDtoSr.getResult());
        model.addAttribute("house", houseDto);

        // es 聚合，该小区共有多少套房源

        return "house-detail";

    }




















    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/


}
