package cn.net.yao.generate.domain;

import cn.net.yao.generate.util.StringUtil;
import java.util.List;

public class TableInfo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private String tableName;
    private String tableComment;
    private ColumnInfo primaryKey;
    private List<ColumnInfo> columns;
    private String className;
    private String classname;
    private boolean isMasterSlave;
    private List<TableInfo> slaveTables;

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getTableComment() { return tableComment; }
    public void setTableComment(String tableComment) { this.tableComment = tableComment; }

    public List<ColumnInfo> getColumns() { return columns; }
    public ColumnInfo getColumnsLast() {
        if (StringUtil.isNotNull(columns) && !columns.isEmpty()) {
            return columns.get(0);
        }
        return null;
    }
    public void setColumns(List<ColumnInfo> columns) { this.columns = columns; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getClassname() { return classname; }
    public void setClassname(String classname) { this.classname = classname; }

    public ColumnInfo getPrimaryKey() { return primaryKey; }
    public void setPrimaryKey(ColumnInfo primaryKey) { this.primaryKey = primaryKey; }

    public boolean isMasterSlave() { return isMasterSlave; }
    public void setMasterSlave(boolean masterSlave) { isMasterSlave = masterSlave; }

    public List<TableInfo> getSlaveTables() { return slaveTables; }
    public void setSlaveTables(List<TableInfo> slaveTables) { this.slaveTables = slaveTables; }
}
