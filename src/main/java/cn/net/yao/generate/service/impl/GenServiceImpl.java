package cn.net.yao.generate.service.impl;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cn.net.yao.generate.config.GenConfig;
import cn.net.yao.generate.domain.TableInfo;
import cn.net.yao.generate.service.IGenService;
import cn.net.yao.generate.util.CharsetKit;
import cn.net.yao.generate.util.GenUtils;
import cn.net.yao.generate.util.SqlParser;
import cn.net.yao.generate.util.StringUtil;
import cn.net.yao.generate.util.VelocityInitializer;

@Service
public class GenServiceImpl implements IGenService {

    private static final Logger logger = LoggerFactory.getLogger(GenServiceImpl.class);

    @Override
    public List<TableInfo> parseSqlToTables(String createSql) {
        return SqlParser.parseMultipleCreateTables(createSql);
    }

    @Override
    public byte[] generatorCodeFromSql(String author, String packageName, String createSql) {
        return generatorCodeFromSql(author, packageName, createSql, null);
    }

    @Override
    public byte[] generatorCodeFromSql(String author, String packageName, String createSql, String[] codeTypes) {
        List<TableInfo> tables = SqlParser.parseMultipleCreateTables(createSql);
        for (TableInfo table : tables) {
            table.setColumns(GenUtils.transColums(table.getColumns()));
            if (table.getColumns() != null && !table.getColumns().isEmpty()) {
                table.setPrimaryKey(table.getColumns().get(0));
            }
        }

        VelocityInitializer.initVelocity();

        String realPackageName = StringUtil.isEmpty(packageName) ? GenConfig.getPackageName() : packageName;
        String moduleName = GenUtils.getModuleName(realPackageName);

        List<String> templates = GenUtils.getTemplates();
        if (codeTypes != null && codeTypes.length > 0) {
            templates = filterTemplatesByCodeTypes(templates, codeTypes);
        }

        boolean hasTestCode = templates.stream().anyMatch(t -> t.contains("Test.java.vm"));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            for (TableInfo table : tables) {
                VelocityContext context = GenUtils.getVelocityContext(author, realPackageName, table);

                for (String template : templates) {
                    try (StringWriter sw = new StringWriter()) {
                        String templateContent = getTemplateContentForRender(template);
                        String renderedContent;
                        if (templateContent != null) {
                            Velocity.evaluate(context, sw, template, templateContent);
                            renderedContent = sw.toString();
                        } else {
                            Template tpl = Velocity.getTemplate(template, CharsetKit.UTF_8);
                            tpl.merge(context, sw);
                            renderedContent = sw.toString();
                            templateContent = getOriginalTemplateContent(template);
                        }

                        String fileName = GenUtils.getFileName(realPackageName, template, table, moduleName,
                                templateContent);
                        if (fileName != null) {
                            zip.putNextEntry(new ZipEntry(fileName));
                            IOUtils.write(renderedContent, zip, CharsetKit.UTF_8);
                            zip.closeEntry();
                        }
                    } catch (Exception e) {
                        logger.error("Render template failed, table: {}", table.getTableName(), e);
                    }
                }

                if (hasTestCode) {
                    generatePomFile(context, realPackageName, table, moduleName, zip);
                }
            }
        } catch (IOException e) {
            logger.error("Generate code ZIP failed", e);
            throw new RuntimeException("Generate code ZIP failed", e);
        }
        return outputStream.toByteArray();
    }

    @Override
    public List<String> getTemplateList() {
        List<String> templates = GenUtils.getTemplates();
        List<String> templateNames = new ArrayList<>();
        for (String template : templates) {
            String[] parts = template.split("/");
            if (parts.length > 0) {
                templateNames.add(parts[parts.length - 1]);
            }
        }
        return templateNames;
    }

    @Override
    public String getTemplateContent(String templateName, String templateDir) {
        try {
            if (templateDir != null && !templateDir.trim().isEmpty()) {
                String customPath = getCustomTemplatePath(templateName, templateDir);
                File customFile = new File(customPath);
                if (customFile.exists()) {
                    return new String(Files.readAllBytes(customFile.toPath()), CharsetKit.UTF_8);
                }
            }

            String customPath = getCustomTemplatePath(templateName, null);
            File customFile = new File(customPath);
            if (customFile.exists()) {
                return new String(Files.readAllBytes(customFile.toPath()), CharsetKit.UTF_8);
            }

            String templatePath = findTemplatePath(templateName);
            if (templatePath == null) {
                throw new RuntimeException("Template not found: " + templateName);
            }

            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(templatePath)) {
                if (inputStream == null) {
                    throw new RuntimeException("Cannot read template: " + templateName);
                }
                return readContentFromStream(inputStream);
            }
        } catch (Exception e) {
            logger.error("Read template failed: {}", templateName, e);
            throw new RuntimeException("Read template failed: " + e.getMessage());
        }
    }

    @Override
    public void saveTemplate(String templateName, String content, String templateDir) {
        try {
            String customPath = getCustomTemplatePath(templateName, templateDir);
            File customDir = new File(customPath).getParentFile();
            if (!customDir.exists()) {
                customDir.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(customPath);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, CharsetKit.UTF_8)) {
                writer.write(content);
            }
            logger.info("Template saved: {}", customPath);
        } catch (Exception e) {
            logger.error("Save template failed: {}", templateName, e);
            throw new RuntimeException("Save template failed: " + e.getMessage());
        }
    }

    @Override
    public String validateTemplate(String templateName, String content) {
        try {
            VelocityInitializer.initVelocity();
            VelocityContext testContext = new VelocityContext();
            testContext.put("tableName", "test_table");
            testContext.put("tableComment", "test table");
            testContext.put("className", "TestTable");
            testContext.put("classname", "testTable");
            testContext.put("moduleName", "test");
            testContext.put("columns", new ArrayList<>());
            testContext.put("basePackage", "com.test");
            testContext.put("package", "com.test");
            testContext.put("author", "test");
            testContext.put("datetime", "2024-01-01 00:00:00");
            testContext.put("primaryKey", null);

            StringWriter sw = new StringWriter();
            Velocity.evaluate(testContext, sw, templateName, content);
            return null; // success
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 200) {
                errorMsg = errorMsg.substring(0, 200) + "...";
            }
            return "Syntax error: " + errorMsg;
        }
    }

    @Override
    public boolean checkCustomTemplate(String templateName, String templateDir) {
        String customPath = templateDir != null && !templateDir.trim().isEmpty()
                ? getCustomTemplatePath(templateName, templateDir)
                : getCustomTemplatePath(templateName, null);
        return new File(customPath).exists();
    }

    @Override
    public byte[] restoreDefaultTemplates(String templateDir) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            List<String> templateNames = getTemplateList();
            String customDir = resolveCustomDir(templateDir);
            int backupCount = 0;

            for (String templateName : templateNames) {
                String customPath = customDir + templateName;
                File customFile = new File(customPath);
                if (customFile.exists() && customFile.isFile()) {
                    zip.putNextEntry(new ZipEntry(templateName));
                    try (FileInputStream fis = new FileInputStream(customFile)) {
                        IOUtils.copy(fis, zip);
                    }
                    zip.closeEntry();
                    customFile.delete();
                    backupCount++;
                }
            }

            File templateDirectory = new File(customDir);
            if (templateDirectory.exists() && templateDirectory.isDirectory()) {
                File[] files = templateDirectory.listFiles();
                if (files == null || files.length == 0) {
                    templateDirectory.delete();
                }
            }
            logger.info("Restored default templates, backed up {} custom templates", backupCount);
        } catch (Exception e) {
            logger.error("Restore default templates failed", e);
        }
        return outputStream.toByteArray();
    }

    // --- Private helper methods ---

    private List<String> filterTemplatesByCodeTypes(List<String> templates, String[] codeTypes) {
        List<String> filtered = new ArrayList<>();
        List<String> codeTypeList = Arrays.asList(codeTypes);

        for (String template : templates) {
            boolean include = false;
            if (template.contains("Entity.java.vm") && codeTypeList.contains("entity")) include = true;
            else if (template.contains("Mapper.java.vm") && codeTypeList.contains("mapper")) include = true;
            else if (template.contains("Mapper.xml.vm") && codeTypeList.contains("xml")) include = true;
            else if (template.contains("Service.java.vm") && codeTypeList.contains("service")) include = true;
            else if (template.contains("Controller.java.vm") && codeTypeList.contains("controller")) include = true;
            else if ((template.contains("api.js.vm") || template.contains("index.vue.vm")) && codeTypeList.contains("vue")) include = true;
            else if ((template.contains("api.ts.vm") || template.contains("react/index.tsx.vm")) && codeTypeList.contains("react")) include = true;
            else if (template.contains("Test.java.vm") && codeTypeList.contains("test")) include = true;
            else if (template.contains("ConditionEntity.java.vm") && codeTypeList.contains("entity")) include = true;

            if (include) filtered.add(template);
        }
        return filtered;
    }

    private String findTemplatePath(String templateName) {
        for (String template : GenUtils.getTemplates()) {
            if (template.endsWith("/" + templateName) || template.endsWith("\\" + templateName)) {
                return template;
            }
        }
        return null;
    }

    private String getCustomTemplatePath(String templateName, String templateDir) {
        String customDir = resolveCustomDir(templateDir);
        return customDir + templateName;
    }

    private String resolveCustomDir(String templateDir) {
        String customDir;
        if (templateDir != null && !templateDir.trim().isEmpty()) {
            customDir = templateDir.trim();
            if (customDir.startsWith("~")) {
                customDir = System.getProperty("user.home") + customDir.substring(1);
            }
        } else {
            customDir = System.getProperty("user.home") + File.separator + ".code-generator" + File.separator + "templates";
        }
        if (!customDir.endsWith(File.separator)) {
            customDir += File.separator;
        }
        return customDir;
    }

    private String getTemplateContentForRender(String templatePath) {
        if (GenConfig.isIgnoreCustomTemplate()) return null;
        try {
            String[] parts = templatePath.split("/");
            String templateName = parts[parts.length - 1];
            String customPath = getCustomTemplatePath(templateName, null);
            File customFile = new File(customPath);
            if (customFile.exists()) {
                return new String(Files.readAllBytes(customFile.toPath()), CharsetKit.UTF_8);
            }
        } catch (Exception e) {
            logger.error("Read custom template failed: {}", templatePath, e);
        }
        return null;
    }

    private String getOriginalTemplateContent(String templatePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(templatePath)) {
            if (inputStream == null) return null;
            return readContentFromStream(inputStream);
        } catch (Exception e) {
            logger.error("Read original template failed: {}", templatePath, e);
            return null;
        }
    }

    private String readContentFromStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, CharsetKit.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    private void generatePomFile(VelocityContext context, String packageName, TableInfo table,
                                  String moduleName, ZipOutputStream zip) {
        try {
            String pomTemplate = "vm/test/pom-dependencies.xml.vm";
            try (StringWriter sw = new StringWriter()) {
                Template tpl = Velocity.getTemplate(pomTemplate, CharsetKit.UTF_8);
                tpl.merge(context, sw);
                String content = sw.toString();
                String fileName = GenUtils.getFileName(packageName, pomTemplate, table, moduleName, null);
                zip.putNextEntry(new ZipEntry(fileName));
                IOUtils.write(content, zip, CharsetKit.UTF_8);
                zip.closeEntry();
            }
        } catch (Exception e) {
            logger.error("Generate POM file failed", e);
        }
    }
}
