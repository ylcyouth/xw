package com.gagaco.xunxuproj2.service.house;


import com.gagaco.xunxuproj2.ApplicationTests;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class QiniuServiceTests extends ApplicationTests {

    @Autowired
    IQiniuService qiniuService;


    /**
     * 上传图片测试
     */
    @Test
    public void uploadFileTest() {
        String filename = "E:\\xunxu-proj2\\tmp\\2018-08-30.jpg";
        File file = new File(filename);
        Assert.assertTrue(file.exists());

        try {
            Response response = qiniuService.uploadFile(file);
            Assert.assertTrue(response.isOK());
        } catch (QiniuException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除图片测试
     */
    @Test
    public void deleteFileTest() {
        String key = "FkjQxwmRhubvw6_4AatypTLEj-Lh";
        try {
            Response response = qiniuService.deleteFile(key);
            Assert.assertTrue(response.isOK());
        } catch (QiniuException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除图片测试
     */
    @Test
    public void deleteFileTest2() {
        List<String> photoList = new ArrayList();
        photoList.add("FgqU1t-1FUOE_kKS1Jr4W0907OjP");
        photoList.add("FlWQzNoNqzq_baBnupLgCworU6iI");
        photoList.add("FnPDiJjkJz_pc5eeiausZ13gQCU1");
        photoList.add("FrNi6sb-UiTYhhVaLqYZJ2qZWvkD");

        try {
            for (String s : photoList) {
                Response response = qiniuService.deleteFile(s);
                Assert.assertTrue(response.isOK());
            }
        } catch (QiniuException e) {
            e.printStackTrace();
        }
    }


}
