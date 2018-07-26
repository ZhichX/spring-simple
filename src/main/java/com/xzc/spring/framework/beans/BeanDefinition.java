package com.xzc.spring.framework.beans;

import com.xzc.spring.framework.annotation.AnnotationUtil;

public class BeanDefinition {
    /**
     * 类的别名
     */
    private String alias = AnnotationUtil.DEFAULT_VALUE;
    /**
     * 类的全限定名
     */
    private String beanClassName;
    /**
     * 简单类名 首字母小写
     */
    private String factoryBeanName;
    /**
     * 是否延迟加载
     */
    private boolean lazyInit = false;

    public String getBeanDefinitionName() {
        return !"".equals(alias) ? alias : beanClassName != null ? beanClassName : factoryBeanName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getBeanClassName() {
        return beanClassName;
    }

    public void setBeanClassName(String beanClassName) {
        this.beanClassName = beanClassName;
    }

    public boolean isLazyInit() {
        return lazyInit;
    }

    public void setLazyInit(boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    public void setFactoryBeanName(String factoryBeanName) {
        this.factoryBeanName = factoryBeanName;
    }
}
