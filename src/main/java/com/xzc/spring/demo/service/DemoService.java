package com.xzc.spring.demo.service;

import com.xzc.spring.framework.annotation.Service;

@Service
public class DemoService {
    public String getInitString() {
        return "hello simple Spring";
    }
}
