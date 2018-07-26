package com.xzc.spring.demo.service.impl;

import com.xzc.spring.demo.service.DemoService;
import com.xzc.spring.framework.annotation.Service;

@Service("demoService")
public class DemoServiceImpl implements DemoService {
    public String getInitString() {
        return "hello simple Spring";
    }
}
