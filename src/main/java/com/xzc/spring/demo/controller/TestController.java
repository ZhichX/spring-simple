package com.xzc.spring.demo.controller;

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

    @RequestMapping("getInfo")
    public ModelAndView getInfo(HttpServletRequest req, HttpServletResponse resp,
                                @RequestParam("name") String name) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", name);
        model.put("data", "return by invoke the method TestController.getInfo");
        return new ModelAndView("test1.html", model);
    }
}
