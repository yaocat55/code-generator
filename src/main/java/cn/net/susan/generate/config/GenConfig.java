package cn.net.susan.generate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"classpath:generator.yml"}, factory = YamlPropertySourceFactory.class)
public class GenConfig {

    private static String author;
    private static String packageName;
    private static String autoRemovePre;
    private static String tablePrefix;
    private static boolean ignoreCustomTemplate = true;

    public static String getAuthor() { return author; }

    @Value("${gen.author:sue}")
    public void setAuthor(String author) { GenConfig.author = author; }

    public static String getPackageName() { return packageName; }

    @Value("${gen.packageName:com.sue}")
    public void setPackageName(String packageName) { GenConfig.packageName = packageName; }

    public static String getAutoRemovePre() { return autoRemovePre; }

    @Value("${gen.autoRemovePre:false}")
    public void setAutoRemovePre(String autoRemovePre) { GenConfig.autoRemovePre = autoRemovePre; }

    public static String getTablePrefix() { return tablePrefix; }

    @Value("${gen.tablePrefix:}")
    public void setTablePrefix(String tablePrefix) { GenConfig.tablePrefix = tablePrefix; }

    public static boolean isIgnoreCustomTemplate() { return ignoreCustomTemplate; }

    @Value("${gen.ignoreCustomTemplate:true}")
    public void setIgnoreCustomTemplate(boolean ignoreCustomTemplate) {
        GenConfig.ignoreCustomTemplate = ignoreCustomTemplate;
    }
}
