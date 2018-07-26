package com.xzc.spring.framework.aop;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * simple aop config
 * 对application中的expression的封装
 * tell Spring which method need to be enhanced
 * <p>
 * aspect 的信息是由用户定义 包括定义的aspect类和待织入的方法逻辑
 * pointCut/method 由框架记录
 */
public class AopConfig {
    /**
     * 以目标对象需要增强的Method作为key，需要增强的信息内容作为value
     */
    Map<Method, DefaultAspect> points = new HashMap<>();

    public DefaultAspect put(Method target, Object aspectClass, Method[] points) {
        return this.points.put(target, new DefaultAspect(aspectClass, points));
    }

    public DefaultAspect get(Method method) {
        return points.get(method);
    }

    public boolean contains(Method method) {
        return points.containsKey(method);
    }

    /**
     * 用于封装用户定义的Aspect信息
     */
    public static class DefaultAspect {
        // 用户定义的aspect类
        private Object aspectClass;
        // 待织入的逻辑方法
        private Method[] points;

        public DefaultAspect(Object aspectClass, Method[] points) {
            this.aspectClass = aspectClass;
            this.points = points;
        }

        public Object getAspectClass() {
            return aspectClass;
        }

        public Method[] getPoints() {
            return points;
        }

    }
}
