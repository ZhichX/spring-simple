package com.xzc.spring.framework.beans;

import com.xzc.spring.framework.core.FactoryBean;

/**
 * Spring中 所有被工厂构造出来的对象都是FactoryBean
 */
public class BeanWrapper implements FactoryBean {
    /**
     * 包装实例
     */
    private Object wrapperInstance;
    /**
     * 原始实例, 通过反射new出来的
     */
    private Object originInstance;
    /**
     * 支持事件响应，会有一个监听 (观察者模式)
     */
    private BeanPostProcessor beanPostProcessor;

    public BeanWrapper(Object wrapperInstance) {
        this.wrapperInstance = wrapperInstance;
        // 暂时设置成一样的
        this.originInstance = wrapperInstance;
    }

    /**
     * 返回代理后的class
     */
    public Class<?> getWrapperedClass() {
        return this.wrapperInstance.getClass();
    }

    public Object getWrapperInstance() {
        return this.wrapperInstance;
    }

    public void setWrapperInstance(Object wrapperInstance) {
        this.wrapperInstance = wrapperInstance;
    }

    public Object getOriginInstance() {
        return this.originInstance;
    }

    public void setOriginInstance(Object originInstance) {
        this.originInstance = originInstance;
    }

    public BeanPostProcessor getBeanPostProcessor() {
        return this.beanPostProcessor;
    }

    public void setBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessor = beanPostProcessor;
    }
}
