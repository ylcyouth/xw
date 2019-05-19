package com.gagaco.xunxuproj2.service.supportaddress;

import com.gagaco.xunxuproj2.ApplicationTests;
import com.gagaco.xunxuproj2.service.ServiceResult;
import com.gagaco.xunxuproj2.service.search.BaiduMapLocation;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author wjj
 * @create 2019/5/19 15:57
 */
public class SupportAddressServiceTest extends ApplicationTests {

    @Autowired
    private ISupportAddressService supportAddressService;

    @Test
    public void getBaiduMapLocationTest() {

        String city = "北京";
        String address = "北京市昌平区巩华家园1号楼2单元";

        ServiceResult<BaiduMapLocation> baiduMapLocation = supportAddressService.getBaiduMapLocation(city, address);

        /*if (baiduMapLocation.isSuccess()) {
            BaiduMapLocation result = baiduMapLocation.getResult();
            //System.out.println("longitude: " + result.getLongitude());
            //System.out.println("latitude: " + result.getLatitude());
        }*/
        Assert.assertTrue(baiduMapLocation.isSuccess());

        Assert.assertTrue(baiduMapLocation.getResult().getLongitude() > 0 );
        Assert.assertTrue(baiduMapLocation.getResult().getLatitude() > 0 );

    }






}
