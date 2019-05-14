package com.gagaco.xunxuproj2.service.house;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;

import java.io.File;
import java.io.InputStream;

/**
 * @time 2019-4-23 22:20:11
 * @author wangjiajia
 *
 *
 */
public interface IQiniuService {

    Response uploadFile(File file) throws QiniuException;

    Response uploadFile(InputStream inputStream) throws QiniuException;

    Response deleteFile(String key) throws QiniuException;




}
