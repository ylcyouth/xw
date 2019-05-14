package com.gagaco.xunxuproj2.base;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * 常用区间定义
 * @date 2019-5-1 23:00:17
 * @author wangjiajia
 *
 */
public class RentValueBlock {

    /**
     * 价格区间
     */
    public static final Map<String, RentValueBlock> PRICE_BLOCK;

    /**
     * 面积区间
     */
    public static final Map<String, RentValueBlock> AREA_BLOCK;

    /**
     * 无限制区间
     */
    public static final RentValueBlock ALL = new RentValueBlock("*", -1, -1);


    static {
        PRICE_BLOCK = ImmutableMap.<String, RentValueBlock>builder()
                .put("*-1500", new RentValueBlock("*-1500", -1, 1500))
                .put("1500-2000", new RentValueBlock("1500-2000", 1500, 2000))
                .put("2000-3000", new RentValueBlock("2000-3000", 2000, 3000))
                .put("3000-5000", new RentValueBlock("3000-5000", 3000, 5000))
                .put("5000-8000", new RentValueBlock("5000-8000", 5000, 8000))
                .put("8000-*", new RentValueBlock("8000-*", 8000, -1))
                .build();
        AREA_BLOCK = ImmutableMap.<String, RentValueBlock>builder()
                .put("*-30", new RentValueBlock("*-30", -1, 30))
                .put("30-50", new RentValueBlock("30-50", 30, 45))
                .put("50-80", new RentValueBlock("50-80", 50, 80))
                .put("80-120", new RentValueBlock("80-120", 80, 120))
                .put("120-*", new RentValueBlock("120-*", 120, -1))
                .build();
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private String key;

    private int min;

    private int max;

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public RentValueBlock(String key, int min, int max) {
        this.key = key;
        this.min = min;
        this.max = max;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    //static方法

    public static RentValueBlock matchPrice(String key) {
        RentValueBlock priceBlock= PRICE_BLOCK.get(key);
        if (priceBlock == null) {
            return ALL;
        }
        return priceBlock;
    }

    public static RentValueBlock matchArea(String key) {
        RentValueBlock areaBlock = AREA_BLOCK.get(key);
        if (areaBlock == null) {
            return ALL;
        }
        return areaBlock;
    }

}
