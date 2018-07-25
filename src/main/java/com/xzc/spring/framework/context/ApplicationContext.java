package com.xzc.spring.framework.context;

import com.xzc.spring.framework.annotation.Autowired;
import com.xzc.spring.framework.annotation.Component;
import com.xzc.spring.framework.annotation.Controller;
import com.xzc.spring.framework.annotation.Service;
import com.xzc.spring.framework.beans.BeanDefinition;
import com.xzc.spring.framework.beans.BeanPostProcessor;
import com.xzc.spring.framework.beans.BeanWrapper;
import com.xzc.spring.framework.context.support.BeanDefinitionReader;
import com.xzc.spring.framework.core.BeanFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext extends DefaultListableBeanFactory implements BeanFactory {
    private String[] configLocations;
    // 用于 资源定位
    private BeanDefinitionReader reader;

    private Map<String, Object> beanCacheMap = new ConcurrentHashMap<>();
    private Map<String, BeanWrapper> beanWrapperMap = new ConcurrentHashMap<>();

    public ApplicationContext(String... locations) {
        configLocations = locations;
        refresh();
    }

    private void refresh() {
        //定位 通过各种BeanDefinitionReader读取相应的类文件到内存中
        reader = new BeanDefinitionReader(configLocations);
        //加载
        List<String> beanDefinitions = reader.loadBeanDefinitions();
        //注册
        doRegistry(beanDefinitions);
        //依赖注入 为每个BeanDefinition生成BeanWrapper，并对他们进行DI依赖关系注入
        doAutowired();
    }

    private void doAutowired() {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            if (!entry.getValue().isLazyInit()) {
                String beanName = entry.getKey();
                // 内部调用getBean 为每个BeanDefinition生成BeanWrapper并放入beanWrapperMap
                getBean(beanName);
            }
        }

        // todo 优化
        for (Map.Entry<String, BeanWrapper> entry : beanWrapperMap.entrySet()) {
            // 为每个instance注入DI关系 (WrapperInstance)
            populateBean(entry.getKey(), entry.getValue().getOriginInstance());
        }
    }

    /**
     * 依赖注入
     *
     * @param beanName 简单类名首字母小写
     * @param instance 待设置DI的原始类
     */
    private void populateBean(String beanName, Object instance) {
        Class<?> clazz = instance.getClass();
        if (!isAnnotationAllowed(clazz)) {
            return;
        }
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(Autowired.class)) {
                continue;
            }
            Autowired autowired = field.getAnnotation(Autowired.class);
            /*
            beanName的三种情况
            1.alias
            2.简单类名首字母小写
            3.接口注入
             */
            String autowiredBeanName = autowired.value().trim();
            if ("".equals(autowiredBeanName)) {
                if ("".equals(beanName) || beanName == null) {
                    autowiredBeanName = field.getType().getName();
                } else {
                    autowiredBeanName = beanName;
                }
            }

            field.setAccessible(true);
            try {
                field.set(instance, this.beanWrapperMap.get(autowiredBeanName).getWrapperInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isAnnotationAllowed(Class<?> clazz) {
        return clazz.isAnnotationPresent(Controller.class)
                || clazz.isAnnotationPresent(Service.class)
                || clazz.isAnnotationPresent(Component.class);
    }

    /**
     * 为每个扫描到的类 生成BeanDefinition
     * 并注册到beanDefinitionMap中
     *
     * @param classNames class的全限定名
     */
    private void doRegistry(List<String> classNames) {
        if (classNames == null || classNames.isEmpty()) {
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> bean = Class.forName(className);
                if (bean.isInterface()) {
                    continue;
                }
                BeanDefinition beanDefinition = reader.registryBean(className);
                if (beanDefinition != null) {
                    synchronized (beanDefinitionMap) {
                        beanDefinitionMap.put(beanDefinition.getFactoryBeanName(), beanDefinition);
                    }
                }
                Class<?>[] interfaces = bean.getInterfaces();
                for (Class<?> i : interfaces) {
                    // 这里 若一个接口有多个实现类是会覆盖的
                    beanDefinitionMap.put(i.getName(), beanDefinition);
                }
                // 至此 bean初始化完毕
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 依赖注入 从这里开始
     * 通过读取BeanDefinition的信息，通过反射创建实例并返回
     * <p>
     * spring中不会把原始对象暴露出去，而是通过BeanWrapper进行一次包装
     * <p>
     * 装饰器模式:
     * 保留原来的OOP关系
     * 对Bean可以进行扩展增强
     *
     * @param beanName
     * @return
     */
    @Override
    public Object getBean(String beanName) {
        BeanDefinition beanDefinition = this.beanDefinitionMap.get(beanName);
        Object instance = instantionBean(beanDefinition);
        if (instance == null) {
            return null;
        }

        // 用于生成事件通知
        BeanPostProcessor beanPostProcessor = new BeanPostProcessor();
        beanPostProcessor.postProcessBeforeInitialization(instance, beanName);

        BeanWrapper beanWrapper = new BeanWrapper(instance);
        beanWrapper.setBeanPostProcessor(beanPostProcessor);
        beanWrapper.setOriginInstance(instance);
        this.beanWrapperMap.put(beanName, beanWrapper);

        beanPostProcessor.postProcessAfterInitialization(instance, beanName);

        //通过这种调用方式，给我们自己留下可操作的空间
        return this.beanWrapperMap.get(beanName).getWrapperInstance();
    }

    private Object instantionBean(BeanDefinition beanDefinition) {
        String className = beanDefinition.getBeanClassName();
        Object instance = null;

        try {
            if (this.beanCacheMap.containsKey(className)) {
                instance = this.beanCacheMap.get(className);
            } else {
                Class<?> klass = Class.forName(className);
                instance = klass.newInstance();
                this.beanCacheMap.put(className, instance);
            }
            return instance;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[] getBeanDefinitionNames() {
        return this.beanDefinitionMap.keySet().toArray(new String[this.beanDefinitionMap.size()]);
    }

    public int getBeanDefinitionCount() {
        return  this.beanDefinitionMap.size();
    }


    public Properties getConfig(){
        return this.reader.getConfig();
    }

}
