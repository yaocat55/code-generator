package cn.net.susan.generate.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import cn.net.susan.generate.domain.GenResult;
import cn.net.susan.generate.domain.SqlGenRequest;
import cn.net.susan.generate.domain.TableInfo;
import cn.net.susan.generate.domain.TemplateRequest;
import cn.net.susan.generate.service.IGenService;

@RestController
@RequestMapping("/api/gen")
public class GenController {

    private static final Logger logger = LoggerFactory.getLogger(GenController.class);
    private final IGenService genService;

    public GenController(IGenService genService) {
        this.genService = genService;
    }

    /**
     * Upload SQL file and parse tables.
     */
    @PostMapping("/upload")
    public GenResult uploadSqlFile(@RequestParam("file") MultipartFile file) {
        try {
            String content = new String(file.getBytes(), "UTF-8");
            List<TableInfo> tables = genService.parseSqlToTables(content);
            return GenResult.ok("Parsed " + tables.size() + " table(s)", tables);
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
            return GenResult.ok("Parsed " + tables.size() + " table(s)", tables);
        } catch (Exception e) {
            logger.error("Parse SQL failed", e);
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
                    request.getCreateSql(), request.getCodeTypes());
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
