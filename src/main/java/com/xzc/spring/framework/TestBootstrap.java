package com.xzc.spring.framework;

import com.xzc.spring.framework.webmvc.servlet.DispatcherServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Enumeration;

public class TestBootstrap {
    public static void main(String[] args) throws ServletException {
        ServletConfig config = new ServletConfig() {
            @Override
            public String getServletName() {
                return "xzc-mvc";
            }

            @Override
            public ServletContext getServletContext() {
                return null;
            }

            @Override
            public String getInitParameter(String s) {
                if ("contextConfigLocation".equals(s)) {
                    return "classpath:application.properties";
                }
                return null;
            }

            @Override
            public Enumeration getInitParameterNames() {
                return null;
            }
        };
        new DispatcherServlet().init(config);
    }
}
