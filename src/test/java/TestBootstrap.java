import com.xzc.spring.framework.webmvc.servlet.DispatcherServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Enumeration;

/**
 * test DispatcherServlet
 */
public class TestBootstrap {
    public static void main(String[] args) throws ServletException {
        new DispatcherServlet().init(new ServletConfig() {
            @Override
            public String getServletName() {
                return "testMVC";
            }

            @Override
            public ServletContext getServletContext() {
                return null;
            }

            @Override
            public String getInitParameter(String s) {
                return "contextConfigLocation".equals(s) ? "classpath:application.properties" : null;
            }

            @Override
            public Enumeration getInitParameterNames() {
                return null;
            }
        });
    }
}
