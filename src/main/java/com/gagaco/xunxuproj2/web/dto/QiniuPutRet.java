package com.gagaco.xunxuproj2.web.dto;

/**
 * @time 2019-4-23 22:52:18
 * @author wangjiajia
 *
 * 这里适合用 public
 */
public class QiniuPutRet {

    public String key;

    public String hash;

    public String bucket;

    public int width;

    public int height;

    @Override
    public String toString() {
        return "QiniuPutRet{" +
                "key='" + key + '\'' +
                ", hash='" + hash + '\'' +
                ", bucket='" + bucket + '\'' +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
