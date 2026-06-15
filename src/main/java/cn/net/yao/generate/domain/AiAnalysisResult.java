package cn.net.yao.generate.domain;

import java.util.List;

public class AiAnalysisResult {

    private String sql;
    private List<String> assumptions;
    private List<AuditWarning> warnings;
    private java.util.Map<String, java.util.Map<String, String>> columnExamples;

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }

    public List<String> getAssumptions() { return assumptions; }
    public void setAssumptions(List<String> assumptions) { this.assumptions = assumptions; }

    public List<AuditWarning> getWarnings() { return warnings; }
    public void setWarnings(List<AuditWarning> warnings) { this.warnings = warnings; }

    public java.util.Map<String, java.util.Map<String, String>> getColumnExamples() { return columnExamples; }
    public void setColumnExamples(java.util.Map<String, java.util.Map<String, String>> columnExamples) { this.columnExamples = columnExamples; }

    public static class AuditWarning {
        private String level;  // error, warning, info
        private String category;
        private String message;
        private String suggestion;

        public AuditWarning() {}

        public AuditWarning(String level, String category, String message, String suggestion) {
            this.level = level;
            this.category = category;
            this.message = message;
            this.suggestion = suggestion;
        }

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
}
