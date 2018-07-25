package com.xzc.spring.framework.webmvc.servlet;

import com.xzc.spring.framework.annotation.Controller;
import com.xzc.spring.framework.annotation.RequestMapping;
import com.xzc.spring.framework.annotation.RequestParam;
import com.xzc.spring.framework.context.ApplicationContext;
import com.xzc.spring.framework.webmvc.HandlerAdapter;
import com.xzc.spring.framework.webmvc.HandlerMapping;
import com.xzc.spring.framework.webmvc.ModelAndView;
import com.xzc.spring.framework.webmvc.ViewResolver;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DispatcherServlet extends HttpServlet {
    private final static String CONTEXT_CONFIG_LOCATION = "contextConfigLocation";
    private final static String TEMPLATE_ROOT = "templateRoot";

    private List<HandlerMapping> handlerMappings = new ArrayList<>();
    private List<HandlerAdapter> handlerAdapters = new ArrayList<>();
    private List<ViewResolver> viewResolvers = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("<font size='30' color='red'>500 Exception</font><br/>Details:<br/>"
                    + Arrays.toString(e.getStackTrace())
                    .replaceAll("\\[|\\]", "")
                    .replaceAll("\\s", "\r\n"));
            e.printStackTrace();
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        // 根据用户请求的URL来获得一个Handler
        HandlerMapping handler = getHandler(req);
        if (handler == null) {
            resp.getWriter().write("<font size='30' color='red'>404 Not Found</font><br/>");
            return;
        }

        HandlerAdapter ha = getHandlerAdapter(handler);

        ModelAndView mv = ha.handle(req, resp, handler);

        processDispatchResult(resp, mv);

        closeResources(resp);
    }

    private void closeResources(HttpServletResponse resp) throws IOException {
        PrintWriter writer = resp.getWriter();
        if (writer != null) {
            writer.close();
        }
    }

    private void processDispatchResult(HttpServletResponse resp, ModelAndView mv) throws IOException {
        if (mv == null) return;
        if (this.viewResolvers.isEmpty()) return;
        for (ViewResolver viewResolver : this.viewResolvers) {
            if (!viewResolver.getName().equals(mv.getViewName())) {
                continue;
            }
            String result = viewResolver.viewResolver(mv);
            if (result != null) {
                resp.getWriter().write(result);
                break;
            }
        }
    }

    private HandlerAdapter getHandlerAdapter(HandlerMapping handler) {
        if (this.handlerAdapters.isEmpty()) {
            throw new IllegalArgumentException("handler mapping error");
        }
        for (HandlerAdapter handlerAdapter : this.handlerAdapters) {
            if (handlerAdapter.getHandlerMapping() == handler) {
                return handlerAdapter;
            }
        }
        throw new IllegalArgumentException("handler mapping error");
    }

    /**
     * 根据用户请求的URL来获得一个Handler
     *
     * @param req HttpServletRequest
     * @return HandlerMapping
     */
    private HandlerMapping getHandler(HttpServletRequest req) {
        if (this.handlerMappings.isEmpty()) return null;

        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();

        uri = uri.replace(contextPath, "").replace("/+", "");
        for (HandlerMapping handlerMapping : this.handlerMappings) {
            Matcher matcher = handlerMapping.getPattern().matcher(uri);
            if (matcher.find()) {
                return handlerMapping;
            }
        }
        return null;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // init IOC container
        String configInitParameter = config.getInitParameter(CONTEXT_CONFIG_LOCATION);
        ApplicationContext context = new ApplicationContext(configInitParameter);

        initStrategies(context);
    }

    /**
     * 9种初始化策略
     * 针对每个用户请求都会经过这些处理策略
     *
     * @param context ApplicationContext
     */
    private void initStrategies(ApplicationContext context) {
        // 文件上传解析，如果请求类型是multipart将通过MultipartResolver进行文件上传解析
        initMultipartResolver(context);
        // 本地化解析
        initLocaleResolver(context);
        // 主题解析
        initThemeResolver(context);

        /*
         HandlerMapping 用来保存Controller中配置的RequestMapping和Method的一个对应关系
         通过HandlerMapping，将请求映射到处理器
         */
        initHandlerMappings(context);
        /*
         HandlerAdapters 用来动态匹配Method参数，包括类转换，动态赋值
         通过HandlerAdapter进行多类型的参数动态匹配
         */
        initHandlerAdapters(context);

        // 如果执行过程中遇到异常，将交给HandlerExceptionResolver来解析
        initHandlerExceptionResolvers(context);
        // 直接解析请求到视图名
        initRequestToViewNameTranslator(context);

        /*
         通过ViewResolvers实现动态模板的解析
         自己解析一套模板语言
         通过viewResolver解析逻辑视图到具体视图实现
         */
        initViewResolvers(context);

        // flash映射管理器
        initFlashMapManager(context);
    }

    /**
     * 将Controller中配置的RequestMapping和Method进行关系映射
     *
     * @param context ApplicationContext
     */
    private void initHandlerMappings(ApplicationContext context) {
        String[] beanNames = context.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object controller = context.getBean(beanName);
            Class<?> controllerClass = controller.getClass();
            // FIXME 加入 AOP 后 这里得到的是代理对象, 需要得到原对象来做处理
            if (!controllerClass.isAnnotationPresent(Controller.class)) {
                continue;
            }

            String basePath = "";
            if (controllerClass.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
                basePath = requestMapping.value();
            }

            // 扫描所有public的带RequestMapping的方法
            Method[] methods = controllerClass.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }
                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String regex = "/" + basePath + "/" + requestMapping.value();
                regex = regex.replaceAll("\\*", ".*").replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);
                this.handlerMappings.add(new HandlerMapping(pattern, controller, method));
                System.out.println("RequestMapping: [" + regex + "] : " + method.getName());
            }
        }
    }

    /**
     * 动态匹配Method参数，包括类转换，动态赋值
     *
     * @param context ApplicationContext
     */
    private void initHandlerAdapters(ApplicationContext context) {
        for (HandlerMapping handlerMapping : this.handlerMappings) {
            Method method = handlerMapping.getMethod();
            Map<String, Integer> paramMap = new HashMap<>(method.getParameterCount());

            setParamMapForName(method, paramMap);
            setParamMapForRequestAndResponse(method, paramMap);
            setParamMapForAnnotation(method, paramMap);

            this.handlerAdapters.add(new HandlerAdapter(paramMap, handlerMapping));
        }
    }

    private void setParamMapForAnnotation(Method method, Map<String, Integer> paramMap) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        // 注解指定的参数名称
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof RequestParam) {
                    String parameterName = ((RequestParam) annotation).value().trim();
                    if (!"".equals(parameterName)) {
                        paramMap.put(parameterName, i);
                    }
                }
            }
        }
    }

    private void setParamMapForRequestAndResponse(Method method, Map<String, Integer> paramMap) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        // 记录 HttpServletRequest & HttpServletResponse
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType == HttpServletRequest.class || parameterType == HttpServletResponse.class) {
                paramMap.put(parameterType.getName(), i);
            }
        }
    }

    private void setParamMapForName(Method method, Map<String, Integer> paramMap) {
        /* java8+的新增函数, 使用编译器时加上-parameters参数
        // 在Spring中的实现是封装了ASM进行参数名称的获取
        // see: org.springframework.core.LocalVariableTableParameterNameDiscoverer
        // todo 同时还需要让paramMap中相同value的key能在后续两次赋值中被覆盖
        */
        /*Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            paramMap.put(parameters[i].getName(), i);
        }*/
    }

    /**
     * 动态模板的解析（简单实现）
     *
     * @param context ApplicationContext
     */
    private void initViewResolvers(ApplicationContext context) {
        String templateRoot = context.getConfig().getProperty(TEMPLATE_ROOT);
        if (templateRoot == null || "".equals(templateRoot)) return;
        URL url = getClass().getClassLoader().getResource(templateRoot);
        if (url == null) return;
        String templateRootPath = url.getFile();
        fileResolve(templateRootPath);
    }

    private void fileResolve(String templateRootPath) {
        File file = new File(templateRootPath);
        File[] files = file.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                fileResolve(f.getPath());
            } else {
                this.viewResolvers.add(new ViewResolver(f.getName(), f));
            }
        }
    }


    /*****************************************************************************/
    private void initFlashMapManager(ApplicationContext context) {
    }

    private void initRequestToViewNameTranslator(ApplicationContext context) {
    }

    private void initHandlerExceptionResolvers(ApplicationContext context) {
    }

    private void initThemeResolver(ApplicationContext context) {
    }

    private void initLocaleResolver(ApplicationContext context) {
    }

    private void initMultipartResolver(ApplicationContext context) {
    }

}
