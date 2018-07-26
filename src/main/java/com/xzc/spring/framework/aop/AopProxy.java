package com.xzc.spring.framework.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * simple AopProxy
 * default proxy implements by JDK dynamic proxy
 */
public class AopProxy implements InvocationHandler {

    private AopConfig aopConfig;
    private Object target;

    public Object getProxy(Object target) {
        this.target = target;
        Class<?> klass = target.getClass();
        return Proxy.newProxyInstance(target.getClass().getClassLoader(), klass.getInterfaces(), this);
    }

    public void setAopConfig(AopConfig aopConfig) {
        this.aopConfig = aopConfig;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method targetMethod = this.target.getClass().getMethod(method.getName(), method.getParameterTypes());

        /*
        调用原始方法前 执行before的增强逻辑
         */
        if (aopConfig.contains(targetMethod)) {
            AopConfig.DefaultAspect aspect = aopConfig.get(targetMethod);
            aspect.getPoints()[0].invoke(aspect.getAspectClass());
        }

        // 反射调用原始的方法
        Object result = method.invoke(this.target, args);
        System.out.println("==> invoke result: " + result);

        /*
        调用原始方法之后，执行After的增强逻辑
         */
        if (aopConfig.contains(targetMethod)) {
            AopConfig.DefaultAspect aspect = aopConfig.get(targetMethod);
            aspect.getPoints()[1].invoke(aspect.getAspectClass());
        }
        return result;
    }
}
