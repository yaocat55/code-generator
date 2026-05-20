package cn.net.susan.generate.domain;

public class TemplateRequest {
    private String templateName;
    private String content;
    private String templateDir;

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTemplateDir() { return templateDir; }
    public void setTemplateDir(String templateDir) { this.templateDir = templateDir; }
}
