package com.xzc.spring.framework.aop;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

public class AopProxyUtil {

    /**
     * 获取目标代理类的原对象
     * 目标类不是代理戴相泽返回其本身
     *
     * @param target 目标代理对象
     * @return 目标代理类的原对象
     * @throws Exception NoSuchFieldException, IllegalAccessException
     */
    public static Object getTargetObject(Object target) throws Exception {
        if (isProxyClass(target)) {
            return getProxyTargetObject(target);
        }
        return target;
    }

    private static boolean isProxyClass(Object target) {
        return Proxy.isProxyClass(target.getClass());
    }

    private static Object getProxyTargetObject(Object proxy) throws NoSuchFieldException, IllegalAccessException {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy) h.get(proxy);
        Field target = aopProxy.getClass().getDeclaredField("target");
        target.setAccessible(true);
        return target.get(aopProxy);
    }
}
