package com.lkl.framework.type;

/**
 * 限流类型
 * 采用{@link Type.DefaultMethodType} 是对整个方法做限流
 * 采用{@link CustomType}时可对一个方法做自定义限流
 * Created by liaokailin on 16/12/13.
 */
public interface Type {

    class DefaultMethodType implements Type {
    }

}
