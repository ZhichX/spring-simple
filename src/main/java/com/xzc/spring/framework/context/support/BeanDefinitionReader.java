package com.xzc.spring.framework.context.support;

import com.xzc.spring.framework.beans.BeanDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 对配置文件进行 查找，读取，解析
 */
public class BeanDefinitionReader {
    private final static String SCAN_PACKAGES = "componentScanPackages";
    private static final String SPLIT_STRING = ",";

    private Properties config = new Properties();

    private List<String> registryBeanClasses = new ArrayList<>();

    public BeanDefinitionReader(String... locations) {
        if (locations == null) {
            throw new IllegalArgumentException("locations is null");
        }
        loadConfigs(locations);
        doScanner(config.getProperty(SCAN_PACKAGES).split(SPLIT_STRING));
    }

    public List<String> loadBeanDefinitions() {
        return registryBeanClasses;
    }

    public BeanDefinition registryBean(String className) {
        if (this.registryBeanClasses.contains(className)) {
            BeanDefinition beanDefinition = new BeanDefinition();
            beanDefinition.setBeanClassName(className);
            String factoryName = lowerFirstCase(className.substring(className.lastIndexOf(".") + 1));
            beanDefinition.setFactoryBeanName(factoryName);
            return beanDefinition;
        }
        return null;
    }

    private void doScanner(String... packageNames) {
        if (packageNames == null) {
            return;
        }
        for (String packageName : packageNames) {
            String resourcePath = packageName.replaceAll("\\.", "/");
            URL url = getClass().getClassLoader().getResource(resourcePath);
            String filePath = url.getFile();
            if (filePath == null) {
                return;
            }
            File fileDir = new File(filePath);
            for (File file : fileDir.listFiles()) {
                String fileName = packageName + "." + file.getName();
                if (file.isDirectory()) {
                    doScanner(fileName);
                } else {
                    registryBeanClasses.add(fileName.replace(".class", ""));
                }
            }
        }
    }

    private void loadConfigs(String[] locations) {
        for (String location : locations) {
            location = location.replace("classpath:", "");
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(location)) {
                config.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String lowerFirstCase(String name) {
        char[] chars = name.toCharArray();
        char c = chars[0];
        if (c > 64 && c < 91) {
            //仅当首字母为大写字母时 转为相应的 小写字母(A~Z的ASCII码值是65～90)
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }

    public Properties getConfig() {
        return config;
    }
}
