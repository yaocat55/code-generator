package cn.net.yao.generate.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.velocity.VelocityContext;
import cn.net.yao.generate.config.GenConfig;
import cn.net.yao.generate.domain.ColumnInfo;
import cn.net.yao.generate.domain.TableInfo;

public class GenUtils {

    private static final String MYBATIS_PATH = "main/resources/mapper";
    private static final String VUE_PATH = "main/resources/templates";
    private static final String REACT_PATH = "main/resources/templates";

    public static Map<String, String> javaTypeMap = new HashMap<>();

    static {
        javaTypeMap.put("tinyint", "Integer");
        javaTypeMap.put("smallint", "Integer");
        javaTypeMap.put("mediumint", "Integer");
        javaTypeMap.put("int", "Integer");
        javaTypeMap.put("number", "Integer");
        javaTypeMap.put("integer", "Integer");
        javaTypeMap.put("bigint", "Long");
        javaTypeMap.put("float", "Float");
        javaTypeMap.put("double", "Double");
        javaTypeMap.put("decimal", "BigDecimal");
        javaTypeMap.put("bit", "Boolean");
        javaTypeMap.put("char", "String");
        javaTypeMap.put("varchar", "String");
        javaTypeMap.put("varchar2", "String");
        javaTypeMap.put("tinytext", "String");
        javaTypeMap.put("text", "String");
        javaTypeMap.put("mediumtext", "String");
        javaTypeMap.put("longtext", "String");
        javaTypeMap.put("time", "Date");
        javaTypeMap.put("date", "Date");
        javaTypeMap.put("datetime", "Date");
        javaTypeMap.put("timestamp", "Date");
    }

    public static List<ColumnInfo> transColums(List<ColumnInfo> columns) {
        List<ColumnInfo> columsList = new ArrayList<>();
        for (ColumnInfo column : columns) {
            String attrName = StringUtil.convertToCamelCase(column.getColumnName());
            column.setAttrName(attrName);
            column.setAttrname(StringUtil.uncapitalize(attrName));
            column.setExtra(column.getExtra());
            String attrType = javaTypeMap.get(column.getDataType());
            column.setAttrType(attrType);
            columsList.add(column);
        }
        return columsList;
    }

    public static VelocityContext getVelocityContext(String author, String packageName, TableInfo table) {
        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("tableName", table.getTableName());
        velocityContext.put("tableComment", replaceKeyword(table.getTableComment()));
        velocityContext.put("primaryKey", table.getPrimaryKey());
        velocityContext.put("className", table.getClassName());
        velocityContext.put("classname", table.getClassname());
        velocityContext.put("moduleName", getModuleName(packageName));
        velocityContext.put("columns", table.getColumns());
        velocityContext.put("basePackage", packageName);
        velocityContext.put("package", packageName);
        velocityContext.put("author", author);
        velocityContext.put("datetime", now());
        velocityContext.put("prefix", getModuleName(packageName));
        return velocityContext;
    }

    private static String now() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }

    public static List<String> getTemplates() {
        List<String> templates = new ArrayList<>();
        // Standard MyBatis
        templates.add("vm/java/Entity.java.vm");
        templates.add("vm/java/ConditionEntity.java.vm");
        templates.add("vm/java/Mapper.java.vm");
        templates.add("vm/java/Service.java.vm");
        templates.add("vm/java/Controller.java.vm");
        templates.add("vm/xml/Mapper.xml.vm");
        templates.add("vm/xml/Mapper-pg.xml.vm");
        templates.add("vm/xml/Mapper-mssql.xml.vm");
        // MyBatis-Plus
        templates.add("vm/java/mp/Entity.java.vm");
        templates.add("vm/java/mp/Mapper.java.vm");
        templates.add("vm/java/mp/Service.java.vm");
        templates.add("vm/java/mp/ServiceImpl.java.vm");
        templates.add("vm/java/mp/Controller.java.vm");
        // JPA
        templates.add("vm/java/jpa/Entity.java.vm");
        templates.add("vm/java/jpa/Repository.java.vm");
        templates.add("vm/java/jpa/Service.java.vm");
        templates.add("vm/java/jpa/ServiceImpl.java.vm");
        templates.add("vm/java/jpa/Controller.java.vm");
        // DDD + Standard MyBatis
        templates.add("vm/java/ddd/domain/Entity.java.vm");
        templates.add("vm/java/ddd/domain/Repository.java.vm");
        templates.add("vm/java/ddd/domain/Condition.java.vm");
        templates.add("vm/java/ddd/infrastructure/RepositoryImpl.java.vm");
        templates.add("vm/java/ddd/infrastructure/Mapper.java.vm");
        templates.add("vm/java/ddd/application/Service.java.vm");
        templates.add("vm/java/ddd/interfaces/Controller.java.vm");
        // DDD + MyBatis-Plus
        templates.add("vm/java/ddd/mp/domain/Entity.java.vm");
        templates.add("vm/java/ddd/mp/domain/Repository.java.vm");
        templates.add("vm/java/ddd/mp/domain/Condition.java.vm");
        templates.add("vm/java/ddd/mp/infrastructure/RepositoryImpl.java.vm");
        templates.add("vm/java/ddd/mp/infrastructure/Mapper.java.vm");
        templates.add("vm/java/ddd/mp/application/Service.java.vm");
        templates.add("vm/java/ddd/mp/interfaces/Controller.java.vm");
        // Frontend
        templates.add("vm/vue/api.js.vm");
        templates.add("vm/vue/index.vue.vm");
        templates.add("vm/react/api.ts.vm");
        templates.add("vm/react/index.tsx.vm");
        // Tests
        templates.add("vm/java/MapperTest.java.vm");
        templates.add("vm/java/ServiceTest.java.vm");
        templates.add("vm/java/ControllerTest.java.vm");
        return templates;
    }

    public static String tableToJava(String tableName) {
        String autoRemovePre = GenConfig.getAutoRemovePre();
        String tablePrefix = GenConfig.getTablePrefix();
        if ("true".equals(autoRemovePre) && StringUtil.isNotEmpty(tablePrefix)) {
            tableName = tableName.replaceFirst(tablePrefix, "");
        }
        return StringUtil.convertToCamelCase(tableName);
    }

    public static String extractClassNameSuffix(String templateContent) {
        if (templateContent == null || templateContent.trim().isEmpty()) return null;
        Pattern pattern = Pattern.compile(
                "(?:public\\s+)?(?:class|interface|enum)\\s+\\$\\{className\\}([A-Z][a-zA-Z0-9]*)");
        Matcher matcher = pattern.matcher(templateContent);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String extractPackagePath(String templateContent) {
        if (templateContent == null || templateContent.trim().isEmpty()) return null;
        Pattern pattern = Pattern.compile("package\\s+\\$\\{package\\}\\.([a-z][a-zA-Z0-9]*)\\s*;");
        Matcher matcher = pattern.matcher(templateContent);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String getFileName(String packageName, String template, TableInfo table, String moduleName) {
        return getFileName(packageName, template, table, moduleName, null, null);
    }

    public static String getFileName(String packageName, String template, TableInfo table, String moduleName,
                                     String templateContent) {
        return getFileName(packageName, template, table, moduleName, templateContent, null);
    }

    public static String getFileName(String packageName, String template, TableInfo table, String moduleName,
                                     String templateContent, String archMode) {
        String classname = table.getClassname();
        String className = table.getClassName();
        String javaPath = getProjectPath(packageName);
        String mybatisPath = MYBATIS_PATH + "/" + moduleName + "/" + className;

        String classNameSuffix = null;
        String packagePath = null;
        if (templateContent != null && !templateContent.trim().isEmpty()) {
            classNameSuffix = extractClassNameSuffix(templateContent);
            packagePath = extractPackagePath(templateContent);
        }

        // DDD mappings (check before generic ones)
        if (template.contains("ddd/domain/Entity.java.vm") || template.contains("ddd/mp/domain/Entity.java.vm")) {
            return javaPath + "domain/" + moduleName + "/" + className + ".java";
        }
        if (template.contains("ddd/domain/Repository.java.vm") || template.contains("ddd/mp/domain/Repository.java.vm")) {
            return javaPath + "domain/" + moduleName + "/" + className + "Repository.java";
        }
        if (template.contains("ddd/domain/Condition.java.vm") || template.contains("ddd/mp/domain/Condition.java.vm")) {
            return javaPath + "domain/" + moduleName + "/" + className + "Condition.java";
        }
        if (template.contains("ddd/infrastructure/Mapper.java.vm") || template.contains("ddd/mp/infrastructure/Mapper.java.vm")) {
            return javaPath + "infrastructure/persistence/" + moduleName + "/" + className + "Mapper.java";
        }
        if (template.contains("ddd/infrastructure/RepositoryImpl.java.vm") || template.contains("ddd/mp/infrastructure/RepositoryImpl.java.vm")) {
            return javaPath + "infrastructure/persistence/" + moduleName + "/" + className + "RepositoryImpl.java";
        }
        if (template.contains("ddd/application/Service.java.vm") || template.contains("ddd/mp/application/Service.java.vm")) {
            return javaPath + "application/" + moduleName + "/" + className + "AppService.java";
        }
        if (template.contains("ddd/interfaces/Controller.java.vm") || template.contains("ddd/mp/interfaces/Controller.java.vm")) {
            return javaPath + "interfaces/rest/" + className + "Controller.java";
        }

        // JPA mappings
        if (template.contains("jpa/Entity.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "entity") + "/" + className + "Entity.java";
        }
        if (template.contains("jpa/Repository.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "repository") + "/" + className + "Repository.java";
        }
        if (template.contains("jpa/Service.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "service") + "/" + className + "Service.java";
        }
        if (template.contains("jpa/ServiceImpl.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "service") + "/impl/" + className + "ServiceImpl.java";
        }
        if (template.contains("jpa/Controller.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "controller") + "/" + className + "Controller.java";
        }

        if (template.contains("ConditionEntity.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "entity") + "/" + className
                    + (classNameSuffix != null ? classNameSuffix : "ConditionEntity") + ".java";
        }
        if (template.contains("mp/Entity.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "entity") + "/" + className + "Entity.java";
        }
        if (template.contains("Entity.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "entity") + "/" + className
                    + (classNameSuffix != null ? classNameSuffix : "Entity") + ".java";
        }
        if (template.contains("mp/Mapper.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "mapper") + "/" + className + "Mapper.java";
        }
        if (template.contains("Mapper.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "mapper") + "/" + className
                    + (classNameSuffix != null ? classNameSuffix : "Mapper") + ".java";
        }
        if (template.contains("mp/Service.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "service") + "/" + className + "Service.java";
        }
        if (template.contains("mp/ServiceImpl.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "service") + "/impl/" + className + "ServiceImpl.java";
        }
        if (template.contains("Service.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "service") + "/" + className
                    + (classNameSuffix != null ? classNameSuffix : "Service") + ".java";
        }
        if (template.contains("mp/Controller.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "controller") + "/" + className + "Controller.java";
        }
        if (template.contains("Controller.java.vm")) {
            return javaPath + (packagePath != null ? packagePath : "controller") + "/" + className
                    + (classNameSuffix != null ? classNameSuffix : "Controller") + ".java";
        }
        if (template.contains("Mapper") && template.endsWith(".xml.vm")) {
            return mybatisPath + "Mapper.xml";
        }
        if (template.contains("api.js.vm")) {
            return VUE_PATH + "/" + classname + "/api.js";
        }
        if (template.contains("index.vue.vm")) {
            return VUE_PATH + "/" + classname + "/index.vue";
        }
        if (template.contains("api.ts.vm")) {
            return REACT_PATH + "/" + classname + "/api.ts";
        }
        if (template.contains("react/index.tsx.vm")) {
            return REACT_PATH + "/" + classname + "/index.tsx";
        }
        if (template.contains("MapperTest.java.vm")) {
            if ("ddd".equals(archMode) || "ddd-mp".equals(archMode)) {
                return "test/java/" + packageName.replace(".", "/") + "/infrastructure/persistence/" + moduleName + "/" + className + "MapperTest.java";
            } else if ("jpa".equals(archMode)) {
                return "test/java/" + packageName.replace(".", "/") + "/repository/" + className + "RepositoryTest.java";
            }
            return "test/java/" + packageName.replace(".", "/") + "/mapper/" + className + "MapperTest.java";
        }
        if (template.contains("ServiceTest.java.vm")) {
            if ("ddd".equals(archMode) || "ddd-mp".equals(archMode)) {
                return "test/java/" + packageName.replace(".", "/") + "/application/" + moduleName + "/" + className + "AppServiceTest.java";
            }
            return "test/java/" + packageName.replace(".", "/") + "/service/" + className + "ServiceTest.java";
        }
        if (template.contains("ControllerTest.java.vm")) {
            if ("ddd".equals(archMode) || "ddd-mp".equals(archMode)) {
                return "test/java/" + packageName.replace(".", "/") + "/interfaces/rest/" + className + "ControllerTest.java";
            }
            return "test/java/" + packageName.replace(".", "/") + "/web/controller/" + className + "ControllerTest.java";
        }
        if (template.contains("pom-dependencies.xml.vm")) {
            return "test/pom-dependencies.xml";
        }
        return null;
    }

    public static String getModuleName(String packageName) {
        int lastIndex = packageName.lastIndexOf(".");
        return StringUtil.substring(packageName, lastIndex + 1, packageName.length());
    }

    public static String getBasePackage(String packageName) {
        int lastIndex = packageName.lastIndexOf(".");
        return StringUtil.substring(packageName, 0, lastIndex);
    }

    public static String getProjectPath(String packageName) {
        return "main/java/" + packageName.replace(".", "/") + "/";
    }

    public static String replaceKeyword(String keyword) {
        return keyword == null ? "" : keyword.replaceAll("(?:表|信息|管理)", "");
    }
}
