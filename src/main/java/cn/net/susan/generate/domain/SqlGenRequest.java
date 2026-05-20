package cn.net.susan.generate.domain;

public class SqlGenRequest {
    private String author;
    private String packageName;
    private String createSql;
    private String[] codeTypes;
    /** SQL file name for informational purposes */
    private String fileName;

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }

    public String getCreateSql() { return createSql; }
    public void setCreateSql(String createSql) { this.createSql = createSql; }

    public String[] getCodeTypes() { return codeTypes; }
    public void setCodeTypes(String[] codeTypes) { this.codeTypes = codeTypes; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
