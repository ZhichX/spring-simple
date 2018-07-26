package com.xzc.spring.framework.annotation;

public class AnnotationUtil {
    public final static String DEFAULT_VALUE = "";

    /**
     * 判断类 是否被框架定义的固定的注解修饰
     *
     * @param clazz Class
     * @return boolean
     */
    public static boolean isAnnotationAllowed(Class<?> clazz) {
        return clazz.isAnnotationPresent(Controller.class)
                || clazz.isAnnotationPresent(Service.class)
                || clazz.isAnnotationPresent(Component.class);
    }

    /**
     * 获取@Target包括ElementType.TYPE注解的value
     *
     * @param clazz Class
     * @return value的值
     */
    public static String getTypeAnnotationValue(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Controller.class)) {
            return clazz.getAnnotation(Controller.class).value();
        } else if (clazz.isAnnotationPresent(Service.class)) {
            return clazz.getAnnotation(Service.class).value();
        } else if (clazz.isAnnotationPresent(Component.class)) {
            return clazz.getAnnotation(Component.class).value();
        } else if (clazz.isAnnotationPresent(RequestMapping.class)) {
            return clazz.getAnnotation(RequestMapping.class).value();
        } else if (clazz.isAnnotationPresent(Repository.class)) {
            return clazz.getAnnotation(Repository.class).value();
        }
        return DEFAULT_VALUE;
    }
}
