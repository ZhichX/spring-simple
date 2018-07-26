package com.xzc.spring.demo.aspect;

public class SimpleAspect {

    public void before() {
        System.out.println("Invoker method before");
    }

    public void after() {
        System.out.println("Invoker method after");
    }
}
