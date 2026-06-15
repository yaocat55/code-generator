package cn.net.yao.generate.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import cn.net.yao.generate.domain.AiAnalysisResult;
import cn.net.yao.generate.domain.AiAnalysisResult.AuditWarning;
import cn.net.yao.generate.domain.TableAuditResult;
import cn.net.yao.generate.service.IAiSqlService;

@Service
public class AiSqlServiceImpl implements IAiSqlService {

    private static final Logger log = LoggerFactory.getLogger(AiSqlServiceImpl.class);

    private final ChatClient chatClient;
    private static final int LONG_DOC_THRESHOLD = 3000;

    public AiSqlServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // ── Pass 1: extract structured requirements from noisy/long documents ──

    private static final String EXTRACT_PROMPT = """
            You are a requirements analyst. Extract database-relevant entities, fields, and relationships from the document below.
            The document may contain noise — skip marketing text, UI descriptions, deployment instructions, and general prose.
            Focus ONLY on what could become database tables and columns.

            Output valid JSON (no markdown, no explanation):
            {
              "entities": [
                {
                  "name": "EntityName",
                  "comment": "实体说明",
                  "fields": [
                    {"name": "field_name", "type": "VARCHAR(50)", "comment": "字段说明", "required": true}
                  ],
                  "relationships": ["关联描述"]
                }
              ],
              "businessRules": ["需要强制保证的唯一性规则 or 状态机 or 金额精度要求"],
              "queryPatterns": ["常见查询场景：按xxx搜索、xxx列表分页"]
            }

            If the document does NOT describe any data entities at all, return {"entities":[]}.
            """;

    // ── Pass 2: generate SQL from requirements ──

    private static final String GENERATE_PROMPT_HEAD = """
            You are a senior backend architect. Generate production-ready CREATE TABLE SQL for **%s** from the requirements below.

            ## Hard Requirements (MUST follow)
            1. Every table MUST include these standard audit fields:
               %s
            2. PRIMARY KEY is always `id`. NO auto_increment / SERIAL / IDENTITY.
            3. %s
            4. Every table and every column MUST have a Chinese COMMENT.
            5. Add UNIQUE KEY / UNIQUE constraint for business unique fields (username, email, code, etc.).
            6. Add INDEX for fields used in WHERE / JOIN / ORDER BY, especially foreign key columns.
            7. Use appropriate data types for the target database.
            8. For status/enum fields, list all possible values in the COMMENT.

            ## For Non-Technical Requirements
            - If the requirement is vague ("用户管理", "订单系统"), infer a reasonable table structure based on common admin-system patterns.
            - If relationships are implied but not specified, create the foreign key columns (e.g., order → user_id).
            - If a field's type is not specified, choose the most appropriate type based on the field name and context.
            - Do NOT ask questions — make reasonable assumptions and document them.

            ## Output Format
            Output a JSON object with 3 fields (no markdown fences, no extra text):
            {
              "sql": "-- all CREATE TABLE statements here, semicolon-separated",
              "assumptions": [
                "假设 user 表的 role 字段设计为枚举类型（admin/editor/viewer），如果实际角色数量较多，建议拆分为独立的 role 表和 user_role 关联表"
              ],
              "columnExamples": {
                "table_name": {
                  "column_name": "示例值",
                  "username": "zhangsan",
                  "age": "28",
                  "amount": "199.99",
                  "status": "1（启用）",
                  "create_user_name": "系统管理员"
                }
              }
            }
            Rules for columnExamples:
            - Include EVERY table and EVERY column, including audit fields
            - Example values must be realistic Chinese examples for business fields
            - For status/enum fields, show the value AND its meaning (e.g. "1（启用）")
            - For ID fields, use placeholder like "1234567890123456"
            - For datetime fields, use "2024-01-15 14:30:00"
            - For audit fields (create_user_*, update_user_*, create_time, update_time, is_del), use realistic system values
            If NO tables can be inferred, set sql to empty string and explain why in assumptions.
            """;

    private String buildGeneratePrompt(String dbType) {
        String dbLabel;
        String auditFieldsDef;
        String dbSpecificRules;
        switch (dbType) {
            case "postgresql" -> {
                dbLabel = "PostgreSQL";
                auditFieldsDef = """
                   - id BIGINT NOT NULL COMMENT '主键ID'
                   - create_user_id BIGINT NOT NULL COMMENT '创建人ID'
                   - create_user_name VARCHAR(50) NOT NULL COMMENT '创建人名称'
                   - create_time TIMESTAMP NOT NULL DEFAULT NOW() COMMENT '创建时间'
                   - update_user_id BIGINT NOT NULL COMMENT '更新人ID'
                   - update_user_name VARCHAR(50) NOT NULL COMMENT '更新人名称'
                   - update_time TIMESTAMP(3) NOT NULL DEFAULT NOW() COMMENT '更新时间'
                   - is_del SMALLINT NOT NULL DEFAULT 0 COMMENT '软删除标记'""";
                dbSpecificRules = """
                   Table-level: No ENGINE/CHARSET clauses. Use double-quote identifiers only for reserved keywords.
                   Types: BIGINT for IDs, TIMESTAMP for datetime, SMALLINT for boolean/status, BOOLEAN for true/false, DECIMAL for money, TEXT for long content.
                   Defaults: use NOW() for timestamps, no ON UPDATE (handled by app layer).
                   Use SERIAL/BIGSERIAL only if auto-increment is explicitly requested by the requirement (avoid otherwise).""";
            }
            case "sqlserver" -> {
                dbLabel = "SQL Server";
                auditFieldsDef = """
                   - id BIGINT NOT NULL COMMENT '主键ID'
                   - create_user_id BIGINT NOT NULL COMMENT '创建人ID'
                   - create_user_name NVARCHAR(50) NOT NULL COMMENT '创建人名称'
                   - create_time DATETIME2 NOT NULL DEFAULT GETDATE() COMMENT '创建时间'
                   - update_user_id BIGINT NOT NULL COMMENT '更新人ID'
                   - update_user_name NVARCHAR(50) NOT NULL COMMENT '更新人名称'
                   - update_time DATETIME2 NOT NULL DEFAULT GETDATE() COMMENT '更新时间'
                   - is_del SMALLINT NOT NULL DEFAULT 0 COMMENT '软删除标记'""";
                dbSpecificRules = """
                   Table-level: No ENGINE/CHARSET clauses. Use square brackets [identifier] only for reserved keywords.
                   Types: BIGINT for IDs, DATETIME2 for datetime, SMALLINT for boolean/status, BIT for true/false, DECIMAL for money, NVARCHAR(n) for strings, NVARCHAR(MAX) for long content.
                   Defaults: use GETDATE() for timestamps. No ON UPDATE equivalent (handled by app layer).""";
            }
            default -> { // mysql
                dbLabel = "MySQL";
                auditFieldsDef = """
                   - id BIGINT NOT NULL COMMENT '主键ID'
                   - create_user_id BIGINT NOT NULL COMMENT '创建人ID'
                   - create_user_name VARCHAR(50) NOT NULL COMMENT '创建人名称'
                   - create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
                   - update_user_id BIGINT NOT NULL COMMENT '更新人ID'
                   - update_user_name VARCHAR(50) NOT NULL COMMENT '更新人名称'
                   - update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间'
                   - is_del TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记'""";
                dbSpecificRules = """
                   Table-level: ENGINE=InnoDB, DEFAULT CHARSET=utf8mb4, COLLATE=utf8mb4_unicode_ci. Use backtick quoting.
                   Types: BIGINT for IDs, DATETIME for datetime, TINYINT for boolean/status, DECIMAL for money, TEXT for long content.
                   Defaults: use CURRENT_TIMESTAMP for timestamps, ON UPDATE CURRENT_TIMESTAMP for update_time.""";
            }
        }
        return String.format(GENERATE_PROMPT_HEAD, dbLabel, auditFieldsDef, dbSpecificRules);
    }

    // ── Pass 3: audit generated SQL for security and robustness ──

    private static final String AUDIT_PROMPT = """
            You are a database security auditor. Review the following CREATE TABLE SQL and identify potential issues.

            Check for:
            1. **Concurrency safety** — Does every table have `update_time` for optimistic locking? Any race-condition-prone designs?
            2. **Data integrity** — Missing NOT NULL on required fields? Missing UNIQUE KEY on business-unique columns? Missing INDEX on FK/query columns?
            3. **Soft delete** — Does every table have `is_del`? Are unique indexes compatible with soft-delete (consider is_del in unique constraint)?
            4. **Data type risks** — VARCHAR without length? Using FLOAT/DOUBLE for money? TEXT where VARCHAR would suffice?
            5. **Index design** — Are there high-frequency query columns without indexes? Any redundant indexes?
            6. **Standard fields** — Are all 8 required audit fields present in every table?
            7. **Naming conventions** — Consistent naming? Reserved keyword conflicts?
            8. **Scalability** — Any anti-patterns for large data volumes?

            ## Output Format
            Output a JSON array (no markdown, no explanation):
            [
              {"level":"error|warning|info", "category":"分类", "message":"问题描述", "suggestion":"修复建议"}
            ]

            Rules:
            - level="error" for critical issues (data loss, security hole, missing required audit fields)
            - level="warning" for best-practice violations (missing index, suboptimal type choice)
            - level="info" for observations and suggestions
            - If no issues found, output []
            - Be specific: reference actual table names and column names
            - Write ALL messages in Chinese
            """;

    // ── Per-table audit ──

    private static final String TABLE_AUDIT_PROMPT = """
            You are a database design auditor. Analyze each CREATE TABLE statement and identify issues per table.

            ## Important: Association/Junction Tables
            Tables that ONLY contain foreign key columns (plus PK) are association/junction tables.
            These tables serve many-to-many relationships (e.g. user_role, role_permission, order_product).
            They have very few columns (typically PK + 2-3 FK columns).
            For association tables, do NOT flag missing audit fields — it is normal for them to lack audit trails.

            ## Audit Checks (per table)
            1. Missing audit fields — ONLY flag for non-association tables. Required: create_user_id, create_user_name, create_time, update_user_id, update_user_name, update_time, is_del
            2. Missing PRIMARY KEY
            3. Missing table COMMENT
            4. Columns missing COMMENT
            5. Potential type issues (varchar without length, float/double for money, missing NOT NULL)
            6. Missing UNIQUE KEY on business-unique columns
            7. Missing INDEX on foreign key / query columns

            ## Output Format
            Output a JSON object (no markdown, no explanation):
            {
              "tables": [
                {
                  "tableName": "table_name",
                  "isAssociation": true,
                  "issues": [
                    {"level":"error|warning|info", "category":"分类", "message":"具体问题", "suggestion":"修复建议"}
                  ]
                }
              ]
            }
            Write all messages in Chinese. If no issues found, issues array is empty.
            """;

    // ── SQL fix prompt ──

    private static final String FIX_PROMPT = """
            You are a database design expert. Fix the following CREATE TABLE SQL according to the instructions.

            Rules:
            1. Output ONLY the fixed SQL statements, no explanation, no markdown fences.
            2. Preserve all existing tables and columns — only modify what the instructions ask for.
            3. Keep the same database dialect (MySQL/PostgreSQL/SQL Server) as the input.
            4. Do NOT add or remove tables unless explicitly asked.
            5. Maintain consistent formatting.
            6. For association/junction tables (tables with only PK + FK columns), do NOT add audit fields.

            Current database type: %s
            """;

    // ── Public API ──

    @Override
    public AiAnalysisResult generateSqlFromRequirement(String requirementText, String dbType) {
        AiAnalysisResult result = new AiAnalysisResult();
        String req = requirementText.trim();
        String realDbType = (dbType == null || dbType.isEmpty()) ? "mysql" : dbType.toLowerCase();

        // Step 1: noise extraction for long documents
        String structuredReq = req;
        if (req.length() > LONG_DOC_THRESHOLD) {
            log.info("Document length {} exceeds threshold {}, running extraction pass", req.length(), LONG_DOC_THRESHOLD);
            structuredReq = extractRequirements(req);
            if (structuredReq.isBlank()) {
                result.setSql("");
                result.setAssumptions(List.of("未能从文档中提取到数据实体信息，请确认文档包含表结构或数据字段描述"));
                result.setWarnings(List.of());
                return result;
            }
            log.info("Extracted structured requirements: {} chars (from {} chars)", structuredReq.length(), req.length());
        }

        // Step 2: generate SQL
        String genResponse = chatClient.prompt()
                .system(buildGeneratePrompt(realDbType))
                .user("需求：\n" + structuredReq)
                .call()
                .content();

        JSONObject genJson = parseJson(genResponse);
        if (genJson == null) {
            result.setSql(cleanSql(genResponse));
            result.setAssumptions(List.of("AI 返回格式异常，已尝试直接从响应中提取 SQL"));
        } else {
            result.setSql(cleanSql(genJson.getString("sql")));
            JSONArray assumptionsArr = genJson.getJSONArray("assumptions");
            result.setAssumptions(assumptionsArr != null ? assumptionsArr.toList(String.class) : List.of());
            // Parse column examples
            JSONObject examplesObj = genJson.getJSONObject("columnExamples");
            if (examplesObj != null) {
                Map<String, Map<String, String>> examples = new HashMap<>();
                for (String tableName : examplesObj.keySet()) {
                    JSONObject cols = examplesObj.getJSONObject(tableName);
                    if (cols != null) {
                        Map<String, String> colMap = new HashMap<>();
                        for (String colName : cols.keySet()) {
                            colMap.put(colName.toLowerCase(), cols.getString(colName));
                        }
                        examples.put(tableName.toLowerCase(), colMap);
                    }
                }
                result.setColumnExamples(examples);
            }
        }

        if (result.getSql().isBlank()) {
            result.setWarnings(List.of());
            return result;
        }

        // Step 3: security audit
        List<AuditWarning> warnings = auditSql(result.getSql());
        result.setWarnings(warnings);

        return result;
    }

    // ── Private helpers ──

    private String extractRequirements(String document) {
        try {
            String resp = chatClient.prompt()
                    .system(EXTRACT_PROMPT)
                    .user("文档内容：\n\n" + document)
                    .call()
                    .content();
            JSONObject json = parseJson(resp);
            if (json != null) {
                JSONArray entities = json.getJSONArray("entities");
                if (entities == null || entities.isEmpty()) {
                    return "";
                }
                // Format back to human-readable for the generate pass
                StringBuilder sb = new StringBuilder();
                sb.append("业务规则：\n");
                JSONArray rules = json.getJSONArray("businessRules");
                if (rules != null && !rules.isEmpty()) {
                    for (int i = 0; i < rules.size(); i++) {
                        sb.append("- ").append(rules.getString(i)).append("\n");
                    }
                }
                sb.append("\n查询场景：\n");
                JSONArray queries = json.getJSONArray("queryPatterns");
                if (queries != null && !queries.isEmpty()) {
                    for (int i = 0; i < queries.size(); i++) {
                        sb.append("- ").append(queries.getString(i)).append("\n");
                    }
                }
                sb.append("\n实体列表：\n");
                for (int i = 0; i < entities.size(); i++) {
                    JSONObject entity = entities.getJSONObject(i);
                    sb.append("\n## ").append(entity.getString("name")).append("\n");
                    sb.append("说明：").append(entity.getString("comment")).append("\n");
                    JSONArray fields = entity.getJSONArray("fields");
                    if (fields != null) {
                        sb.append("字段：\n");
                        for (int j = 0; j < fields.size(); j++) {
                            JSONObject f = fields.getJSONObject(j);
                            sb.append("- ").append(f.getString("name"))
                                    .append(" (").append(f.getString("type")).append(")")
                                    .append(" — ").append(f.getString("comment"))
                                    .append(f.getBooleanValue("required") ? " [必填]" : "").append("\n");
                        }
                    }
                    JSONArray rels = entity.getJSONArray("relationships");
                    if (rels != null && !rels.isEmpty()) {
                        sb.append("关联：\n");
                        for (int j = 0; j < rels.size(); j++) {
                            sb.append("- ").append(rels.getString(j)).append("\n");
                        }
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.warn("Extraction pass failed, using original document", e);
        }
        // On failure, truncate to avoid context overflow
        return document.length() > 8000 ? document.substring(0, 8000) : document;
    }

    private List<AuditWarning> auditSql(String sql) {
        try {
            String resp = chatClient.prompt()
                    .system(AUDIT_PROMPT)
                    .user("审查以下 SQL：\n\n" + sql)
                    .call()
                    .content();
            JSONArray arr = parseJsonArray(resp);
            if (arr != null) {
                List<AuditWarning> warnings = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject w = arr.getJSONObject(i);
                    warnings.add(new AuditWarning(
                            w.getString("level"),
                            w.getString("category"),
                            w.getString("message"),
                            w.getString("suggestion")));
                }
                return warnings;
            }
        } catch (Exception e) {
            log.warn("Audit pass failed", e);
        }
        return List.of();
    }

    @Override
    public List<TableAuditResult> auditTables(String sql, String dbType) {
        if (sql == null || sql.isBlank()) return List.of();
        try {
            String resp = chatClient.prompt()
                    .system(TABLE_AUDIT_PROMPT)
                    .user("审查以下 SQL（数据库类型：" + (dbType != null ? dbType : "mysql") + "）：\n\n" + sql)
                    .call()
                    .content();
            JSONObject json = parseJson(resp);
            if (json != null) {
                JSONArray arr = json.getJSONArray("tables");
                if (arr != null) {
                    List<TableAuditResult> results = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        JSONObject t = arr.getJSONObject(i);
                        TableAuditResult r = new TableAuditResult();
                        r.setTableName(t.getString("tableName"));
                        r.setAssociation(t.getBooleanValue("isAssociation"));
                        JSONArray issuesArr = t.getJSONArray("issues");
                        if (issuesArr != null && !issuesArr.isEmpty()) {
                            List<AuditWarning> issues = new ArrayList<>();
                            for (int j = 0; j < issuesArr.size(); j++) {
                                JSONObject w = issuesArr.getJSONObject(j);
                                issues.add(new AuditWarning(
                                        w.getString("level"),
                                        w.getString("category"),
                                        w.getString("message"),
                                        w.getString("suggestion")));
                            }
                            r.setIssues(issues);
                        } else {
                            r.setIssues(List.of());
                        }
                        results.add(r);
                    }
                    return results;
                }
            }
            log.warn("Table audit: failed to parse AI response as JSON, response length: {}", resp != null ? resp.length() : 0);
        } catch (Exception e) {
            log.warn("Table audit failed", e);
        }
        return List.of();
    }

    @Override
    public String fixSql(String sql, String instructions, String dbType) {
        if (sql == null || sql.isBlank()) return "";
        String realDbType = (dbType == null || dbType.isEmpty()) ? "mysql" : dbType.toLowerCase();
        try {
            String resp = chatClient.prompt()
                    .system(String.format(FIX_PROMPT, realDbType))
                    .user("原始 SQL：\n\n" + sql + "\n\n修复要求：\n" + instructions)
                    .call()
                    .content();
            return cleanSql(resp);
        } catch (Exception e) {
            log.warn("SQL fix failed", e);
            return sql; // fallback to original
        }
    }

    // ── JSON parsing ──

    private JSONObject parseJson(String text) {
        if (text == null) return null;
        String s = text.trim();
        // Strip markdown fences
        s = s.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        // Find the first { and matching }
        int start = s.indexOf('{');
        if (start < 0) return null;
        int braceCount = 0;
        int end = -1;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') {
                braceCount--;
                if (braceCount == 0) { end = i + 1; break; }
            }
        }
        if (end < 0) return null;
        try {
            return JSON.parseObject(s.substring(start, end));
        } catch (Exception e) {
            log.debug("JSON parse failed: {}", e.getMessage());
            return null;
        }
    }

    private JSONArray parseJsonArray(String text) {
        if (text == null) return null;
        String s = text.trim();
        s = s.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        int start = s.indexOf('[');
        if (start < 0) return null;
        int braceCount = 0;
        int end = -1;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') braceCount++;
            else if (c == ']') {
                braceCount--;
                if (braceCount == 0) { end = i + 1; break; }
            }
        }
        if (end < 0) return null;
        try {
            return JSON.parseArray(s.substring(start, end));
        } catch (Exception e) {
            log.debug("JSON array parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String cleanSql(String sql) {
        if (sql == null) return "";
        String s = sql.trim();
        s = s.replaceAll("```sql\\s*", "").replaceAll("```\\s*", "");
        return s.trim();
    }
}
