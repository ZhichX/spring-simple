package com.xzc.spring.framework.context;

import com.xzc.spring.framework.beans.BeanDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultListableBeanFactory extends AbstractApplicationContext {

    /**
     * beanDefinitionMap用来保存配置信息
     * K - V : 简单类名(首字母小写) - BeanDefinition
     */
    protected Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>();

    protected void onRefresh() {
    }

    @Override
    protected void refreshBeanFactory() {

    }
}
