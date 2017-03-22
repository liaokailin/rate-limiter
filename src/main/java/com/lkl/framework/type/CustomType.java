package com.lkl.framework.type;

/**
 * 自定义限流类型
 * Created by liaokailin on 16/12/13.
 */
public interface CustomType extends Type {

    /**
     *
     * @return 返回值用来区分自定义的限流策略,例如:按service token限流时 可返回clientId
     */
    String name();
}
