package com.gagaco.xunxuproj2.service;

import java.util.List;

/**
 * @time 2019-4-24 23:00:34
 * @author wangjiajia
 *
 * service层 通用的 返回结构
 *
 *
 */
public class ServiceMultiResult<T> {

    private long total;

    private List<T> result;

    public ServiceMultiResult(long total, List<T> result) {
        this.total = total;
        this.result = result;
    }


    public int getResultSize() {
        if (this.result == null) {
            return 0;
        }
        return this.result.size();
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getResult() {
        return result;
    }

    public void setResult(List<T> result) {
        this.result = result;
    }
}
