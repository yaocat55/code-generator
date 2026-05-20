package cn.net.susan.generate.service;

import java.util.List;

import cn.net.susan.generate.domain.TableInfo;

public interface IGenService {

    byte[] generatorCodeFromSql(String author, String packageName, String createSql);

    byte[] generatorCodeFromSql(String author, String packageName, String createSql, String[] codeTypes);

    List<TableInfo> parseSqlToTables(String createSql);

    List<String> getTemplateList();

    String getTemplateContent(String templateName, String templateDir);

    void saveTemplate(String templateName, String content, String templateDir);

    String validateTemplate(String templateName, String content);

    boolean checkCustomTemplate(String templateName, String templateDir);

    byte[] restoreDefaultTemplates(String templateDir);
}
