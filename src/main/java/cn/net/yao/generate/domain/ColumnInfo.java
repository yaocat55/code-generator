package cn.net.yao.generate.domain;

public class ColumnInfo {
    private String columnName;
    private String dataType;
    private String columnComment;
    private ColumnConfigInfo configInfo;
    private String attrType;
    private String attrName;
    private String attrname;
    private String extra;

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    public String getColumnComment() { return columnComment; }
    public void setColumnComment(String columnComment) { this.columnComment = columnComment; }

    public String getAttrName() { return attrName; }
    public void setAttrName(String attrName) { this.attrName = attrName; }

    public String getAttrname() { return attrname; }
    public void setAttrname(String attrname) { this.attrname = attrname; }

    public String getAttrType() { return attrType; }
    public void setAttrType(String attrType) { this.attrType = attrType; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }

    public ColumnConfigInfo getConfigInfo() { return configInfo; }
    public void setConfigInfo(ColumnConfigInfo configInfo) { this.configInfo = configInfo; }
}
