package cn.net.yao.generate.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.net.yao.generate.domain.ColumnInfo;
import cn.net.yao.generate.domain.TableInfo;

/**
 * SQL parser - parses CREATE TABLE statements to extract table and column metadata.
 */
public class SqlParser {

    /**
     * Parse a single CREATE TABLE statement.
     */
    public static TableInfo parseCreateTable(String createSql) {
        if (StringUtil.isEmpty(createSql)) {
            throw new RuntimeException("SQL cannot be empty");
        }

        String sql = createSql.trim().replaceAll("\\s+", " ");
        String sqlLower = sql.toLowerCase();

        String tableName = extractTableName(sql, sqlLower);
        if (StringUtil.isEmpty(tableName)) {
            throw new RuntimeException("Cannot extract table name from SQL");
        }

        String tableComment = extractTableComment(sql, sqlLower);

        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableName);
        tableInfo.setTableComment(tableComment != null ? tableComment : "");

        List<ColumnInfo> columns = extractColumns(sql, sqlLower);
        tableInfo.setColumns(columns);

        ColumnInfo primaryKey = findPrimaryKey(sql, sqlLower, columns);
        tableInfo.setPrimaryKey(primaryKey);

        String className = GenUtils.tableToJava(tableName);
        tableInfo.setClassName(className);
        tableInfo.setClassname(StringUtil.uncapitalize(className));

        return tableInfo;
    }

    /**
     * Parse multiple CREATE TABLE statements from a single SQL string.
     */
    public static List<TableInfo> parseMultipleCreateTables(String sql) {
        List<TableInfo> tables = new ArrayList<>();
        String[] statements = splitStatements(sql);
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (StringUtil.isNotEmpty(trimmed) && trimmed.toLowerCase().contains("create table")) {
                try {
                    tables.add(parseCreateTable(trimmed));
                } catch (Exception e) {
                    // Skip invalid statements, continue parsing others
                }
            }
        }
        if (tables.isEmpty()) {
            throw new RuntimeException("No valid CREATE TABLE statements found in SQL");
        }
        return tables;
    }

    /**
     * Split SQL into individual statements.
     */
    private static String[] splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if ((c == '\'' || c == '"') && (i == 0 || sql.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                    quoteChar = 0;
                }
            }
            if (c == ';' && !inQuotes) {
                if (current.length() > 0) {
                    statements.add(current.toString().trim());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            statements.add(current.toString().trim());
        }
        return statements.toArray(new String[0]);
    }

    private static String extractTableName(String sql, String sqlLower) {
        Pattern pattern = Pattern.compile(
                "create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?(?:`)?([a-zA-Z_][a-zA-Z0-9_]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sqlLower);
        if (matcher.find()) {
            int start = matcher.start(1);
            int end = matcher.end(1);
            return sql.substring(start, end).replaceAll("`", "");
        }
        return null;
    }

    private static String extractTableComment(String sql, String sqlLower) {
        Pattern pattern = Pattern.compile("comment\\s*[=:]\\s*['\"]([^'\"]*)['\"]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static List<ColumnInfo> extractColumns(String sql, String sqlLower) {
        List<ColumnInfo> columns = new ArrayList<>();
        int startBracket = sqlLower.indexOf("(");
        int endBracket = findMatchingBracket(sql, startBracket);
        if (startBracket == -1 || endBracket == -1) {
            throw new RuntimeException("Cannot find table definition in SQL");
        }

        String tableDefinition = sql.substring(startBracket + 1, endBracket);
        List<String> fieldDefinitions = splitFieldDefinitions(tableDefinition);

        for (String fieldDef : fieldDefinitions) {
            ColumnInfo column = parseColumn(fieldDef.trim());
            if (column != null) {
                columns.add(column);
            }
        }
        return columns;
    }

    private static List<String> splitFieldDefinitions(String definition) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < definition.length(); i++) {
            char c = definition.charAt(i);
            if ((c == '\'' || c == '"') && (i == 0 || definition.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                    quoteChar = 0;
                }
            }
            if (!inQuotes) {
                if (c == '(') depth++;
                else if (c == ')') depth--;
                else if (c == ',' && depth == 0) {
                    if (current.length() > 0) {
                        fields.add(current.toString().trim());
                        current = new StringBuilder();
                        continue;
                    }
                }
            }
            current.append(c);
        }
        if (current.length() > 0) {
            fields.add(current.toString().trim());
        }
        return fields;
    }

    private static ColumnInfo parseColumn(String fieldDef) {
        fieldDef = fieldDef.trim();
        String fieldLower = fieldDef.toLowerCase();

        if (fieldLower.startsWith("primary key") || fieldLower.startsWith("key ")
                || fieldLower.startsWith("index ") || fieldLower.startsWith("unique ")
                || fieldLower.startsWith("constraint ")) {
            return null;
        }

        ColumnInfo column = new ColumnInfo();
        String columnName = extractColumnName(fieldDef);
        if (StringUtil.isEmpty(columnName)) return null;
        column.setColumnName(columnName);

        String dataType = extractDataType(fieldDef);
        column.setDataType(dataType != null ? dataType.toLowerCase() : "varchar");

        String comment = extractColumnComment(fieldDef);
        column.setColumnComment(comment);

        // 雪花ID策略，不使用数据库自增
        column.setExtra("");

        String attrType = GenUtils.javaTypeMap.get(column.getDataType());
        if (attrType == null) attrType = "String";
        column.setAttrType(attrType);

        String attrName = StringUtil.convertToCamelCase(columnName);
        column.setAttrName(attrName);
        column.setAttrname(StringUtil.uncapitalize(attrName));

        return column;
    }

    private static String extractColumnName(String fieldDef) {
        Pattern pattern = Pattern.compile("^[`]?([a-zA-Z_][a-zA-Z0-9_]*)[`]?");
        Matcher matcher = pattern.matcher(fieldDef.trim());
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractDataType(String fieldDef) {
        Pattern pattern = Pattern.compile("\\s+([a-z]+)(?:\\([^)]+\\))?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fieldDef);
        return matcher.find() ? matcher.group(1).toLowerCase() : "varchar";
    }

    private static String extractColumnComment(String fieldDef) {
        Pattern pattern = Pattern.compile("comment\\s+['\"]([^'\"]*)['\"]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(fieldDef);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static ColumnInfo findPrimaryKey(String sql, String sqlLower, List<ColumnInfo> columns) {
        // 从 PRIMARY KEY 约束中查找
        Pattern pattern = Pattern.compile("primary\\s+key\\s*\\([`\"\\[]?([a-zA-Z_][a-zA-Z0-9_]*)[`\"\\]]?\\)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find()) {
            String pkColumnName = matcher.group(1);
            for (ColumnInfo column : columns) {
                if (pkColumnName.equalsIgnoreCase(column.getColumnName())) return column;
            }
        }
        // 没找到则默认第一列为主键
        return !columns.isEmpty() ? columns.get(0) : null;
    }

    /**
     * Detect the database dialect from SQL syntax characteristics.
     */
    public static String detectDbType(String sql) {
        if (sql == null || sql.isBlank()) return "mysql";
        String upper = sql.toUpperCase();

        // SQL Server: bracket quoting + SQL Server-specific types/functions
        boolean hasBracketQuoting = sql.contains("[") && sql.contains("]");
        boolean hasSqlServerMarkers = upper.contains("NVARCHAR") || upper.contains("DATETIME2")
                || upper.contains("GETDATE()") || upper.contains("IDENTITY(");
        if (hasBracketQuoting && hasSqlServerMarkers) return "sqlserver";
        if (hasSqlServerMarkers && !sql.contains("`") && !upper.contains("ENGINE=")) return "sqlserver";

        // PostgreSQL: PG-specific types without MySQL indicators
        boolean hasPgMarkers = upper.contains("SERIAL") || upper.contains("::")
                || upper.contains("BOOLEAN") || upper.contains("TIMESTAMP WITH TIME ZONE");
        boolean hasMySqlMarkers = sql.contains("`") || upper.contains("ENGINE=")
                || upper.contains("AUTO_INCREMENT") || upper.contains("TINYINT");
        if (hasPgMarkers && !hasMySqlMarkers) return "postgresql";

        return "mysql";
    }

    private static int findMatchingBracket(String sql, int startIndex) {
        int depth = 1;
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = startIndex + 1; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if ((c == '\'' || c == '"') && (i == 0 || sql.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                    quoteChar = 0;
                }
            }
            if (!inQuotes) {
                if (c == '(') depth++;
                else if (c == ')') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }
        return -1;
    }
}
