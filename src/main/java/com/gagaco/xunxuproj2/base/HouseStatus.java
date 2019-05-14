package com.gagaco.xunxuproj2.base;

/**
 * @time 2019-4-27 21:48:22
 * @author wangjiajia
 *
 * 房源的状态枚举类
 *
 */
public enum HouseStatus {

    /**
     * 未审核
     */
    NOT_AUDITED(0),
    /**
     * 审核通过
     */
    PASSES(1),
    /**
     * 已出租
     */
    RENTED(2),
    /**
     * 逻辑删除
     */
    DELETED(3);

    private int value;

    HouseStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
