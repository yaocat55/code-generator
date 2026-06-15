package cn.net.yao.generate.service;

import java.util.List;

import cn.net.yao.generate.domain.AiAnalysisResult;
import cn.net.yao.generate.domain.TableAuditResult;

public interface IAiSqlService {
    AiAnalysisResult generateSqlFromRequirement(String requirementText, String dbType);

    List<TableAuditResult> auditTables(String sql, String dbType);

    String fixSql(String sql, String instructions, String dbType);
}
