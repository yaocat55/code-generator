package cn.net.yao.generate.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import cn.net.yao.generate.domain.AiAnalysisResult;
import cn.net.yao.generate.domain.ColumnInfo;
import cn.net.yao.generate.domain.GenResult;
import cn.net.yao.generate.domain.SqlGenRequest;
import cn.net.yao.generate.domain.TableAuditResult;
import cn.net.yao.generate.domain.TableInfo;
import cn.net.yao.generate.domain.TemplateRequest;
import cn.net.yao.generate.service.IAiSqlService;
import cn.net.yao.generate.service.IGenService;
import cn.net.yao.generate.util.DocumentReader;
import cn.net.yao.generate.util.SqlParser;

@RestController
@RequestMapping("/api/gen")
public class GenController {

    private static final Logger logger = LoggerFactory.getLogger(GenController.class);
    private final IGenService genService;
    private final IAiSqlService aiSqlService;

    public GenController(IGenService genService, IAiSqlService aiSqlService) {
        this.genService = genService;
        this.aiSqlService = aiSqlService;
    }

    /**
     * Upload SQL file and parse tables.
     */
    @PostMapping("/upload")
    public GenResult uploadSqlFile(@RequestParam("file") MultipartFile file) {
        try {
            String content = new String(file.getBytes(), "UTF-8");
            List<TableInfo> tables = genService.parseSqlToTables(content);
            String dbType = SqlParser.detectDbType(content);
            Map<String, Object> result = new HashMap<>();
            result.put("tables", tables);
            result.put("dbType", dbType);
            return GenResult.ok("Parsed " + tables.size() + " table(s) (detected: " + dbType + ")", result);
        } catch (Exception e) {
            logger.error("Upload SQL parse failed", e);
            return GenResult.fail(e.getMessage());
        }
    }

    /**
     * Parse SQL text and return table info.
     */
    @PostMapping("/parse")
    public GenResult parseSql(@RequestBody Map<String, String> body) {
        try {
            String sql = body.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                return GenResult.fail("SQL cannot be empty");
            }
            List<TableInfo> tables = genService.parseSqlToTables(sql);
            String dbType = SqlParser.detectDbType(sql);
            Map<String, Object> result = new HashMap<>();
            result.put("tables", tables);
            result.put("dbType", dbType);
            return GenResult.ok("Parsed " + tables.size() + " table(s) (detected: " + dbType + ")", result);
        } catch (Exception e) {
            logger.error("Parse SQL failed", e);
            return GenResult.fail(e.getMessage());
        }
    }

    /**
     * AI analyze requirement document (file upload) and generate SQL.
     */
    @PostMapping("/ai/analyze")
    public GenResult aiAnalyze(@RequestParam("file") MultipartFile file,
                               @RequestParam(value = "dbType", defaultValue = "mysql") String dbType) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return GenResult.fail("File name is empty");
        }
        String ext = filename.toLowerCase();
        if (!ext.endsWith(".docx") && !ext.endsWith(".md") && !ext.endsWith(".txt")) {
            return GenResult.fail("Unsupported file format, supported formats: .docx, .md, .txt");
        }
        try {
            String text = DocumentReader.readDocument(file);
            return doAiAnalyze(text, dbType);
        } catch (Exception e) {
            logger.error("AI analyze file failed", e);
            return GenResult.fail(e.getMessage());
        }
    }

    /**
     * AI analyze from plain text requirement.
     */
    @PostMapping("/ai/analyze-text")
    public GenResult aiAnalyzeText(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.trim().isEmpty()) {
            return GenResult.fail("Requirement text cannot be empty");
        }
        String dbType = body.getOrDefault("dbType", "mysql");
        return doAiAnalyze(text.trim(), dbType);
    }

    private static final java.util.Set<String> AUDIT_FIELDS = java.util.Set.of(
            "id", "create_user_id", "create_user_name", "create_time",
            "update_user_id", "update_user_name", "update_time", "is_del");

    private GenResult doAiAnalyze(String requirementText, String dbType) {
        try {
            if (requirementText.isBlank()) {
                return GenResult.fail("File content is empty");
            }

            AiAnalysisResult aiResult = aiSqlService.generateSqlFromRequirement(requirementText, dbType);
            List<TableInfo> tables = genService.parseSqlToTables(aiResult.getSql());

            // Enrich columns: mark audit fields and attach examples
            Map<String, Map<String, String>> examples = aiResult.getColumnExamples();
            for (TableInfo table : tables) {
                Map<String, String> tableExamples = examples != null
                        ? examples.getOrDefault(table.getTableName().toLowerCase(), java.util.Collections.emptyMap())
                        : java.util.Collections.emptyMap();
                for (ColumnInfo col : table.getColumns()) {
                    col.setAudit(AUDIT_FIELDS.contains(col.getColumnName().toLowerCase()));
                    col.setExample(tableExamples.getOrDefault(col.getColumnName().toLowerCase(), defaultExample(col)));
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("sql", aiResult.getSql());
            result.put("tables", tables);
            result.put("assumptions", aiResult.getAssumptions());
            result.put("warnings", aiResult.getWarnings());
            return GenResult.ok("AI generated " + tables.size() + " table(s)", result);
        } catch (Exception e) {
            logger.error("AI analyze failed", e);
            return GenResult.fail(e.getMessage());
        }
    }

    private String defaultExample(ColumnInfo col) {
        String type = col.getDataType().toLowerCase();
        String name = col.getColumnName().toLowerCase();
        if (name.contains("id")) return "1234567890123456";
        if (name.contains("time")) return "2024-01-15 14:30:00";
        if (name.contains("is_") || name.contains("status") || type.startsWith("tinyint")) return "0";
        if (type.startsWith("bigint")) return "1234567890123456";
        if (type.startsWith("decimal")) return "199.99";
        if (type.startsWith("int")) return "100";
        if (type.startsWith("varchar") || type.startsWith("char")) return "示例文本";
        if (type.startsWith("text")) return "较长文本内容...";
        if (type.startsWith("datetime") || type.startsWith("timestamp")) return "2024-01-15 14:30:00";
        if (type.startsWith("date")) return "2024-01-15";
        return "—";
    }

    /**
     * AI audit existing SQL — per-table issue analysis.
     */
    @PostMapping("/ai/audit")
    public GenResult aiAudit(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return GenResult.fail("SQL cannot be empty");
        }
        String dbType = body.getOrDefault("dbType", "mysql");
        try {
            List<TableAuditResult> auditResults = aiSqlService.auditTables(sql.trim(), dbType);
            int totalIssues = auditResults.stream().mapToInt(t -> t.getIssues().size()).sum();
            Map<String, Object> result = new HashMap<>();
            result.put("tables", auditResults);
            result.put("totalIssues", totalIssues);
            return GenResult.ok("Audit complete: " + totalIssues + " issue(s) across " + auditResults.size() + " table(s)", result);
        } catch (Exception e) {
            logger.error("AI audit failed", e);
            return GenResult.fail(e.getMessage());
        }
    }

    /**
     * AI fix SQL based on audit issues.
     */
    @PostMapping("/ai/fix")
    public GenResult aiFix(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return GenResult.fail("SQL cannot be empty");
        }
        String instructions = body.getOrDefault("instructions", "修复所有发现的问题");
        String dbType = body.getOrDefault("dbType", "mysql");
        try {
            String fixedSql = aiSqlService.fixSql(sql.trim(), instructions, dbType);
            List<TableInfo> tables = genService.parseSqlToTables(fixedSql);
            Map<String, Object> result = new HashMap<>();
            result.put("sql", fixedSql);
            result.put("tables", tables);
            return GenResult.ok("SQL fixed, parsed " + tables.size() + " table(s)", result);
        } catch (Exception e) {
            logger.error("AI fix failed", e);
            return GenResult.fail(e.getMessage());
        }
    }

    /**
     * Generate code from SQL and return ZIP.
     */
    @PostMapping("/generate")
    public void generateCode(HttpServletResponse response, @RequestBody SqlGenRequest request) throws IOException {
        try {
            byte[] data = genService.generatorCodeFromSql(
                    request.getAuthor(), request.getPackageName(),
                    request.getCreateSql(), request.getCodeTypes(),
                    request.getDbType());
            writeZip(response, data);
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    /**
     * Get template list.
     */
    @GetMapping("/templates")
    public GenResult getTemplateList() {
        return GenResult.ok("ok", genService.getTemplateList());
    }

    /**
     * Get template content.
     */
    @GetMapping("/template")
    public GenResult getTemplateContent(@RequestParam("name") String name,
                                        @RequestParam(value = "dir", required = false) String dir) {
        try {
            String content = genService.getTemplateContent(name, dir);
            Map<String, Object> m = new HashMap<>();
            m.put("name", name);
            m.put("content", content);
            return GenResult.ok("ok", m);
        } catch (Exception e) {
            return GenResult.fail(e.getMessage());
        }
    }

    /**
     * Save template.
     */
    @PostMapping("/template")
    public GenResult saveTemplate(@RequestBody TemplateRequest request) {
        try {
            genService.saveTemplate(request.getTemplateName(), request.getContent(), request.getTemplateDir());
            return GenResult.ok("Template saved");
        } catch (Exception e) {
            return GenResult.fail(e.getMessage());
        }
    }

    /**
     * Validate template syntax.
     */
    @PostMapping("/template/validate")
    public GenResult validateTemplate(@RequestBody TemplateRequest request) {
        String error = genService.validateTemplate(request.getTemplateName(), request.getContent());
        if (error == null) {
            return GenResult.ok("Template syntax is valid");
        }
        return GenResult.fail(error);
    }

    /**
     * Check if custom template exists.
     */
    @GetMapping("/template/custom")
    public GenResult checkCustomTemplate(@RequestParam("name") String name,
                                         @RequestParam(value = "dir", required = false) String dir) {
        boolean exists = genService.checkCustomTemplate(name, dir);
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("custom", exists);
        return GenResult.ok("ok", m);
    }

    /**
     * Restore default templates.
     */
    @PostMapping("/templates/restore")
    public void restoreTemplates(HttpServletResponse response,
                                  @RequestParam(value = "dir", required = false) String dir) throws IOException {
        byte[] zipData = genService.restoreDefaultTemplates(dir);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String filename = "custom-templates-backup-" + timestamp + ".zip";
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.addHeader("Content-Length", "" + zipData.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IOUtils.write(zipData, response.getOutputStream());
    }

    private void writeZip(HttpServletResponse response, byte[] data) throws IOException {
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"code-generator.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IOUtils.write(data, response.getOutputStream());
    }

    private void handleException(HttpServletResponse response, Exception e) throws IOException {
        logger.error("Code generation failed", e);
        response.reset();
        response.setStatus(500);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
