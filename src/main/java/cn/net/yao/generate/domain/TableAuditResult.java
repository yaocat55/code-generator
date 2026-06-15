package cn.net.yao.generate.domain;

import java.util.List;

public class TableAuditResult {

    private String tableName;
    private boolean association; // 关联表/中间表
    private List<AiAnalysisResult.AuditWarning> issues;

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public boolean isAssociation() { return association; }
    public void setAssociation(boolean association) { this.association = association; }

    public List<AiAnalysisResult.AuditWarning> getIssues() { return issues; }
    public void setIssues(List<AiAnalysisResult.AuditWarning> issues) { this.issues = issues; }
}
