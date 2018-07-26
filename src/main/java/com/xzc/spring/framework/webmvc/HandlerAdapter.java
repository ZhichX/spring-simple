package com.xzc.spring.framework.webmvc;

import com.alibaba.fastjson.JSON;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

public class HandlerAdapter {

    private Map<String, Integer> paramMapping;
    private HandlerMapping handlerMapping;

    public HandlerAdapter(Map<String, Integer> paramMapping, HandlerMapping handlerMapping) {
        this.paramMapping = paramMapping;
        this.handlerMapping = handlerMapping;
    }

    public Map<String, Integer> getParamMapping() {
        return paramMapping;
    }

    public HandlerMapping getHandlerMapping() {
        return handlerMapping;
    }

    public ModelAndView handle(HttpServletRequest req, HttpServletResponse resp, HandlerMapping handler) throws InvocationTargetException, IllegalAccessException, IOException {
        // 1、形参列表
        Class<?>[] paramTypes = handler.getMethod().getParameterTypes();
        // URL传过来的参数列表
        Map<String, String[]> reqParamMap = req.getParameterMap();
        // 构造参数列表
        //这里只是简单处理，映射简单类型的值，其他类型映射需要做相应的convert
        Object[] args = new Object[paramTypes.length];
        for (Map.Entry<String, String[]> entry : reqParamMap.entrySet()) {
            String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]", "");
            if (!this.paramMapping.containsKey(entry.getKey())) {
                continue;
            }
            Integer index = this.paramMapping.get(entry.getKey());
            // simple parameter type converter
            args[index] = convertValue(value, paramTypes[index]);
        }

        String name = HttpServletRequest.class.getName();
        if (this.paramMapping.containsKey(name)) {
            Integer i = this.paramMapping.get(name);
            args[i] = req;
        }

        name = HttpServletResponse.class.getName();
        if (this.paramMapping.containsKey(name)) {
            Integer i = this.paramMapping.get(name);
            args[i] = resp;
        }

        // handler.getMethod() 拿到的是原始对象的Method
        Object result = handler.getMethod().invoke(handler.getController(), args);
        // 简单处理
        if (handler.getMethod().getReturnType() == ModelAndView.class) {
            return (ModelAndView) result;
        }
        // default
        resp.getWriter().write(JSON.toJSONString(result));
        return null;
    }

    /**
     * Simple Type Convert
     */
    private Object convertValue(String value, Class<?> clazz) {
        if (clazz == String.class) {
            return value;
        } else if (clazz == Byte.class || clazz == byte.class) {
            return Byte.valueOf(value).byteValue();
        } else if (clazz == Character.class || clazz == char.class) {
            return value.charAt(0);
        } else if (clazz == Short.class || clazz == short.class) {
            return Short.valueOf(value).shortValue();
        } else if (clazz == Integer.class || clazz == int.class) {
            return Integer.valueOf(value).intValue();
        } else if (clazz == Long.class || clazz == long.class) {
            return Long.valueOf(value).longValue();
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return Boolean.valueOf(value).booleanValue();
        } else if (clazz == Double.class || clazz == double.class) {
            return Double.valueOf(value).doubleValue();
        } else if (clazz == Float.class || clazz == float.class) {
            return Float.valueOf(value).floatValue();
        } else {
            return null;
        }
    }
}
