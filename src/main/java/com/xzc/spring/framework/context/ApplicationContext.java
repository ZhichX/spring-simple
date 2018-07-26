package com.xzc.spring.framework.context;

import com.xzc.spring.framework.annotation.AnnotationUtil;
import com.xzc.spring.framework.annotation.Autowired;
import com.xzc.spring.framework.aop.AopConfig;
import com.xzc.spring.framework.beans.BeanDefinition;
import com.xzc.spring.framework.beans.BeanPostProcessor;
import com.xzc.spring.framework.beans.BeanWrapper;
import com.xzc.spring.framework.context.support.BeanDefinitionReader;
import com.xzc.spring.framework.core.BeanFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApplicationContext extends DefaultListableBeanFactory implements BeanFactory {

    private final static String POINT_CUT = "pointCut";
    private final static String ASPECT_BEFORE = "aspectBefore";
    private final static String ASPECT_AFTER = "aspectAfter";

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
        //注册 --> beanDefinitionMap
        doRegistry(beanDefinitions);
        //依赖注入 为每个BeanDefinition生成BeanWrapper，并对他们进行DI依赖关系注入
        doAutowired();
    }

    private void doAutowired() {
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            if (!entry.getValue().isLazyInit()) {
                String beanName = entry.getKey();
                // 内部调用getBean 为每个BeanDefinition生成BeanWrapper并放入beanWrapperMap
                if (!entry.getValue().isLazyInit()) {
                    getBean(beanName);
                }
            }
        }

        // todo 优化
        for (Map.Entry<String, BeanWrapper> entry : beanWrapperMap.entrySet()) {
            // 为每个instance注入DI关系 (WrapperInstance)
            populateBean(entry.getValue().getOriginInstance());
        }
    }

    /**
     * 依赖注入
     *
     * @param instance 待设置DI关系的原始类
     */
    private void populateBean(Object instance) {
        Class<?> clazz = instance.getClass();
        if (!AnnotationUtil.isAnnotationAllowed(clazz)) {
            return;
        }
        Field[] fields = clazz.getDeclaredFields();
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
            if (AnnotationUtil.DEFAULT_VALUE.equals(autowiredBeanName)) {
                autowiredBeanName = field.getType().getName();
            }
            BeanWrapper beanWrapper = this.beanWrapperMap.get(autowiredBeanName);
            if (beanWrapper == null) {
                throw new IllegalArgumentException("bean [" + autowiredBeanName + "] not exist");
            }

            field.setAccessible(true);
            try {
                field.set(instance, beanWrapper.getWrapperInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
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
                // 略过 接口
                if (bean.isInterface()) continue;

                BeanDefinition beanDefinition = reader.registryBean(AnnotationUtil.getTypeAnnotationValue(bean), className);
                if (beanDefinition != null) {
                    String beanDefinitionName = beanDefinition.getBeanDefinitionName();
                    BeanDefinition definition = beanDefinitionMap.putIfAbsent(beanDefinitionName, beanDefinition);
                    if (definition != null) {
                        throw new RuntimeException("repeat bean registry : " + beanDefinitionName);
                    }
                }

                Class<?>[] interfaces = bean.getInterfaces();
                for (Class<?> i : interfaces) {
                    // 这里 若一个接口有多个实现类是会覆盖的
                    beanDefinitionMap.putIfAbsent(i.getName(), beanDefinition);
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
        Object instance = initializationBean(beanDefinition);
        if (instance == null) {
            return null;
        }

        try {
            // 用于生成事件通知
            BeanPostProcessor beanPostProcessor = new BeanPostProcessor();
            beanPostProcessor.postProcessBeforeInitialization(instance, beanName);

            // 设置代理对象和原始对象
            BeanWrapper beanWrapper = new BeanWrapper(instance);
            beanWrapper.setBeanPostProcessor(beanPostProcessor);

            beanWrapper.setAopConfig(initAopConfig(beanDefinition));
            this.beanWrapperMap.put(beanName, beanWrapper);

            beanPostProcessor.postProcessAfterInitialization(instance, beanName);

            // 通过这种调用方式，给我们自己留下可操作的空间
            return this.beanWrapperMap.get(beanName).getWrapperInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AopConfig initAopConfig(BeanDefinition beanDefinition) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException {
        AopConfig aopConfig = new AopConfig();
        Properties config = reader.getConfig();
        String expression = config.getProperty(POINT_CUT);
        String[] aspectBefore = config.getProperty(ASPECT_BEFORE).split("\\s");
        String[] aspectAfter = config.getProperty(ASPECT_AFTER).split("\\s");

        String beanClassName = beanDefinition.getBeanClassName();
        Class<?> clazz = Class.forName(beanClassName);

        /*
        为了简单起见，这里并没有独立出一个expression来解析，
        而是简单使用了正则匹配来粗糙的完成
         */
        Pattern pattern = Pattern.compile(expression);
        Class<?> aspectClass = Class.forName(aspectBefore[0]);
        for (Method method : clazz.getMethods()) {
            Matcher matcher = pattern.matcher(method.toGenericString());
            if (matcher.matches()) {
                // 将满足切面规则的类，放入AOP配置中
                aopConfig.put(method,
                        aspectClass.newInstance(),
                        new Method[]{aspectClass.getMethod(aspectBefore[1]), aspectClass.getMethod(aspectAfter[1])});
            }
        }
        return aopConfig;
    }

    private Object initializationBean(BeanDefinition beanDefinition) {
        String className = beanDefinition.getBeanClassName();
        Object instance;

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
        return this.beanDefinitionMap.size();
    }


    public Properties getConfig() {
        return this.reader.getConfig();
    }

}
