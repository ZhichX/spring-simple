package com.xzc.spring.framework.webmvc;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple ViewResolver
 */
public class ViewResolver {
    private final static Pattern PATTERN = Pattern.compile("@\\{(.+?)\\}", Pattern.CASE_INSENSITIVE);
    private String name;
    private File file;

    public ViewResolver(String name, File file) {
        this.name = name;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public String viewResolver(ModelAndView mv) throws IOException {
        String line = null;
        StringBuilder sb = new StringBuilder();
        try (RandomAccessFile templateFile = new RandomAccessFile(this.file, "r")) {
            while ((line = templateFile.readLine()) != null) {
                line = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                Matcher m = match(line);
                while (m.find()) {
                    for (int i = 1; i <= m.groupCount(); i++) {
                        String name = m.group(i);
                        Object value = mv.getModel().get(name);
                        line = line.replaceAll("@\\{" + name + "}", value.toString());
                    }
                }
                line = new String(line.getBytes("utf-8"), "ISO-8859-1");
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private Matcher match(String line) {
        return PATTERN.matcher(line);
    }
}
