package com.gagaco.xunxuproj2.service.search;

import com.gagaco.xunxuproj2.ApplicationTests;
import com.gagaco.xunxuproj2.service.ServiceMultiResult;
import com.gagaco.xunxuproj2.web.form.RentSearch;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SearchTest extends ApplicationTests {

    @Autowired
    private ISearchService searchService;

    @Test
    public void testIndex2() {
        Long houseId = 17L;
        searchService.index(houseId);
    }

    @Test
    public void testRemove() {
        Long houseId = 16L;
        searchService.remove(houseId);
    }


    @Test
    public void testQuery() {
        RentSearch rentSearch = new RentSearch();
        rentSearch.setCityEnName("bj");
        rentSearch.setStart(0);
        rentSearch.setSize(5);
        ServiceMultiResult<Long> smr = searchService.query(rentSearch);
        Assert.assertEquals(3, smr.getTotal());
    }
}
