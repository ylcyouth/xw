package com.gagaco.xunxuproj2.web.controller.admin;

import com.gagaco.xunxuproj2.base.ApiDataTableResponse;
import com.gagaco.xunxuproj2.base.ApiResponse;
import com.gagaco.xunxuproj2.base.HouseOperation;
import com.gagaco.xunxuproj2.base.HouseStatus;
import com.gagaco.xunxuproj2.entity.SupportAddress;
import com.gagaco.xunxuproj2.service.ServiceMultiResult;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.service.house.IHouseService;
import com.gagaco.xunxuproj2.service.house.IQiniuService;
import com.gagaco.xunxuproj2.service.supportaddress.ISupportAddressService;
import com.gagaco.xunxuproj2.web.dto.*;
import com.gagaco.xunxuproj2.web.form.DataTableSearch;
import com.gagaco.xunxuproj2.web.form.HouseForm;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.websocket.server.PathParam;
import java.io.InputStream;
import java.util.Map;

/**
 * @author wangjiajia
 * @time 2019-4-21 16:39:20
 * @ RequestParam("xxx")
 */
@Controller
public class AdminController {

    @Autowired
    private IQiniuService qiniuService;

    @Autowired
    private Gson gson;

    @Autowired
    private ISupportAddressService supportAddressService;

    @Autowired
    private IHouseService houseService;

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    //一些页面的url

    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "admin/login";
    }

    @GetMapping("/admin/center")
    public String adminCenterPage() {
        return "admin/center";
    }

    @GetMapping("/admin/welcome")
    public String adminWelcomePage() {
        return "admin/welcome";
    }


    @GetMapping("admin/add/house")
    public String addHousePage() {
        return "admin/house-add";
    }

    @GetMapping("admin/house/list")
    public String houseListPage() {
        return "admin/house-list";
    }

    @GetMapping("/admin/house/subscribe")
    public String houseSubscribePage() {
        return "admin/subscribe";
    }


    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    /**
     * 上传图片接口, 这个是在写上传到七牛云的接口之前，写的上传到本地的接口，上传到本地成功之后修改这个接口加入上传到七牛云的代码
     * @param file
     * @return
     */
    /*
    @PostMapping(value = "admin/upload/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ApiResponse uploadPhoto(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }

        String fileName = file.getOriginalFilename();
        File target = new File("E:\\xunxu-proj2\\tmp\\" + fileName);

        try {
            file.transferTo(target);
        } catch (IOException e) {
            e.printStackTrace();
            return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
        }
        return ApiResponse.ofSuccess(null);
    }
    */

    /**
     * 上传图片接口，上传到七牛云
     * 2019-4-23 今天刚注册的七牛云 实名认证还没有通过，先把这个方法注掉，暂时用上面那个方法，把图片上传到本地
     */
    @PostMapping(value = "/admin/upload/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ApiResponse uploadPhoto(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }

        try {
            InputStream inputStream = file.getInputStream();
            Response response = qiniuService.uploadFile(inputStream);
            if (response.isOK()) {
                QiniuPutRet ret = gson.fromJson(response.bodyString(), QiniuPutRet.class);
                return ApiResponse.ofSuccess(ret);
            } else {
                return ApiResponse.ofMessage(response.statusCode, response.getInfo());
            }
        } catch (QiniuException e) {
            Response response = e.response;
            try {
                return ApiResponse.ofMessage(response.statusCode, response.bodyString());
            } catch (QiniuException e1) {
                e1.printStackTrace();
                return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            return ApiResponse.ofStatus(ApiResponse.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    /**
     * 增加 house 接口
     */
    @PostMapping("admin/add/house")
    @ResponseBody
    public ApiResponse addHouse(
            @Valid @ModelAttribute("form-house-add") HouseForm houseForm,
            BindingResult bindingResult) {

        //...
        if (bindingResult.hasErrors()) {
            return new ApiResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    bindingResult.getAllErrors().get(0).getDefaultMessage(),
                    null);
        }

        //没上传房屋图片
        if (houseForm.getPhotos() == null || houseForm.getCover() == null) {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), "必须上传图片");
        }

        Map<SupportAddress.Level, SupportAddressDto> addressDtoMap =
                supportAddressService.findCityAndRegion(houseForm.getCityEnName(), houseForm.getRegionEnName());
        //...
        if (addressDtoMap.keySet().size() != 2) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }

        ServiceResult<HouseDto> result = houseService.save(houseForm);
        //增加house成功
        if (result.isSuccess()) {
            return ApiResponse.ofSuccess(result.getResult());
        }

        //其他情况
        return ApiResponse.ofSuccess(ApiResponse.Status.NOT_VALID_PARAM);
    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    /**
     * 房源列表接口
     */
    @PostMapping("admin/houses")
    @ResponseBody
    public ApiDataTableResponse houses(@ModelAttribute DataTableSearch searchBody) {

        ServiceMultiResult<HouseDto> result = houseService.adminQuery(searchBody);

        ApiDataTableResponse response = new ApiDataTableResponse(ApiResponse.Status.SUCCESS);
        response.setData(result.getResult());
        response.setRecordsTotal(result.getTotal());
        response.setRecordsFiltered(result.getTotal());
        response.setDraw(searchBody.getDraw());
        return response;

    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    /**
     * 房源编辑页面
     */
    @GetMapping("admin/house/edit")
    public String houseEditPage(@PathParam(value = "id") Long id, Model model) {

        //防御
        if (id == null || id < 1) {
            return "404";
        }

        //查询完整的房源信息 房源基本信息 + 详情 + ···
        ServiceResult<HouseDto> houseDtoServiceResult = houseService.findCompleteOne(id);
        if (!houseDtoServiceResult.isSuccess()) {
            return "404";
        }

        //获取查询到的houseDto，放到model中
        HouseDto houseDto = houseDtoServiceResult.getResult();
        model.addAttribute("house", houseDto);

        //根据houseDto中的城市名和区域名查询城市对象和区域对象，放到model中
        Map<SupportAddress.Level, SupportAddressDto> addressDtoMap =
                supportAddressService.findCityAndRegion(houseDto.getCityEnName(), houseDto.getRegionEnName());
        model.addAttribute("city", addressDtoMap.get(SupportAddress.Level.CITY));
        model.addAttribute("region", addressDtoMap.get(SupportAddress.Level.REGION));

        //获取houseDto中的houseDetailDto
        HouseDetailDto houseDetailDto = houseDto.getHouseDetail();

        //根据houseDetailDto中的地铁线id查询地铁线,放到model中
        ServiceResult<SubwayDto> subwayDtoServiceResult =
                supportAddressService.findSubway(houseDetailDto.getSubwayLineId());
        if (subwayDtoServiceResult.isSuccess()) {
            model.addAttribute("subway", subwayDtoServiceResult.getResult());
        }

        //根据houseDetailDto中的地铁站id查询地铁线站,放到model中
        ServiceResult<SubwayStationDto> subwayStationDtoServiceResult =
                supportAddressService.findSubwayStation(houseDetailDto.getSubwayStationId());
        if (subwayStationDtoServiceResult.isSuccess()) {
            model.addAttribute("station", subwayStationDtoServiceResult.getResult());
        }

        //返回房源编辑页面
        return "admin/house-edit";
    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    /**
     * 房源编辑接口
     */
    @PostMapping("admin/house/edit")
    @ResponseBody
    public ApiResponse updateHouse(
            @Valid @ModelAttribute("form-house-edit") HouseForm houseForm, BindingResult bindingResult) {

        //首先判断绑定是否正确
        if (bindingResult.hasErrors()) {
            return new ApiResponse(
                    HttpStatus.BAD_REQUEST.value(), bindingResult.getAllErrors().get(0).getDefaultMessage(), null);
        }

        //接下来判断城市和区域参数是否合法
        Map<SupportAddress.Level, SupportAddressDto> supportAddressDtoMap =
                supportAddressService.findCityAndRegion(houseForm.getCityEnName(), houseForm.getRegionEnName());
        if (supportAddressDtoMap.keySet().size() != 2) {
            return ApiResponse.ofSuccess(ApiResponse.Status.NOT_VALID_PARAM);
        }

        //修改房源
        ServiceResult result = houseService.update(houseForm);
        //判断修改结果
        if (!result.isSuccess()) {
            ApiResponse apiResponse = ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
            apiResponse.setMessage(result.getMessage());

        }

        //到这就是修改成功了，创建接口统一返回结果，返回，接口完成
        return ApiResponse.ofSuccess(null);
    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    /**
     * 增加房源标签接口
     */
    @PostMapping("admin/house/tag")
    @ResponseBody
    public ApiResponse addTag(@RequestParam(value = "house_id") Long houseId,
                              @RequestParam(value = "tag") String tag) {
        //首先判断这两个参数
        if (houseId < 1 || Strings.isNullOrEmpty(tag)) {
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }

        //调用给house添加标签的服务
        ServiceResult serviceResult = houseService.addTag(houseId, tag);

        //判断服务调用结果
        if (serviceResult.isSuccess()) {
            return ApiResponse.ofStatus(ApiResponse.Status.SUCCESS);
        } else {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), serviceResult.getMessage());
        }
    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    /**
     * 移除房源标签接口
     */
    @DeleteMapping("admin/house/tag")
    @ResponseBody
    public ApiResponse removeTag(@RequestParam(value = "house_id") Long houseId,
                                 @RequestParam(value = "tag") String tag) {
        //首先判断这两个参数
        if (houseId < 1 || Strings.isNullOrEmpty(tag)) {
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }

        //调用移除house的标签的服务
        ServiceResult serviceResult = houseService.removeTag(houseId, tag);

        //判断服务调用结果
        if (serviceResult.isSuccess()) {
            return ApiResponse.ofStatus(ApiResponse.Status.SUCCESS);
        } else {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), serviceResult.getMessage());
        }
    }



    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    /**
     * 移除房源图片接口
     */
    @DeleteMapping("admin/house/photo")
    @ResponseBody
    public ApiResponse removePhoto(@RequestParam(value = "id") Long id) {

        //首先判断这个参数
        if (id < 1 || id == null) {
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }

        //调用服务层移除房源图片的服务
        ServiceResult serviceResult = houseService.removePhoto(id);

        //判断服务调用结果，根据结果封装对应的apiResponse
        if (serviceResult.isSuccess()) {
            return ApiResponse.ofStatus(ApiResponse.Status.SUCCESS);
        } else {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), serviceResult.getMessage());
        }
    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    /**
     * 修改封面接口
     *
     * @param coverId housePictureId
     */
    @PostMapping("admin/house/cover")
    @ResponseBody
    public ApiResponse changeCover(@RequestParam(value = "target_id") Long houseId,
                                   @RequestParam(value = "cover_id") Long coverId) {

        //首先判断这两个参数
        if (houseId < 1 || coverId < 1) {
            return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }

        //调用服务层修改房源封面的服务
        ServiceResult serviceResult = houseService.updateCover(houseId, coverId);

        //判断服务调用结果，根据结果封装对应的apiResponse
        if (serviceResult.isSuccess()) {
            return ApiResponse.ofStatus(ApiResponse.Status.SUCCESS);
        } else {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), serviceResult.getMessage());
        }
    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/

    /**
     * 房源操作接口
     * @param id 房源id
     */
    @PutMapping("admin/house/operate/{id}/{operation}")
    @ResponseBody
    public ApiResponse operateHouse(@PathVariable(value = "id") Long id,
                                    @PathVariable(value = "operation") int operation) {

        //首先判断houseId参数
        if (id < 1) {
            return ApiResponse.ofStatus(ApiResponse.Status.NOT_VALID_PARAM);
        }

        //判断操作类型

        //因为下面有多个操作，但是只执行其中一个，先定义一个ServiceResult，调用服务的结果都用它来接收
        ServiceResult serviceResult;

        //根据operation执行对应的操作
        switch (operation) {
            case HouseOperation.PASS:
                serviceResult = houseService.updateStatus(id, HouseStatus.PASSES.getValue());
                break;
            case HouseOperation.PULL_OUT:
                serviceResult = houseService.updateStatus(id, HouseStatus.NOT_AUDITED.getValue());
                break;
            case HouseOperation.DELETE:
                serviceResult = houseService.updateStatus(id, HouseStatus.DELETED.getValue());
                break;
            case HouseOperation.RENT:
                serviceResult = houseService.updateStatus(id, HouseStatus.RENTED.getValue());
            default:
                return ApiResponse.ofStatus(ApiResponse.Status.BAD_REQUEST);
        }

        //判断服务调用结果，根据结果封装对应的apiResponse
        if (serviceResult.isSuccess()) {
            return ApiResponse.ofSuccess(null);
        } else {
            return ApiResponse.ofMessage(HttpStatus.BAD_REQUEST.value(), serviceResult.getMessage());
        }
    }

    /*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
}
