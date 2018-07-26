package com.xzc.spring.demo.controller;

import com.xzc.spring.demo.service.DemoService;
import com.xzc.spring.framework.annotation.Autowired;
import com.xzc.spring.framework.annotation.Controller;
import com.xzc.spring.framework.annotation.RequestMapping;
import com.xzc.spring.framework.annotation.RequestParam;
import com.xzc.spring.framework.webmvc.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("test")
public class TestController {

    @Autowired("demoService")
    private DemoService demoService;

    @RequestMapping("getInfo")
    public ModelAndView getInfo(HttpServletRequest req, HttpServletResponse resp,
                                @RequestParam("name") String name) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", name);
        model.put("data", "return by invoke the method TestController.getInfo");
        return new ModelAndView("test1.html", model);
    }

    @RequestMapping("aop")
    public String aop(@RequestParam("name") String name) {
        String result = demoService.getInitString();
        return result + ", request parameter: " + name;
    }
}
