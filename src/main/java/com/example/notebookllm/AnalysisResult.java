package com.example.notebookllm;

import java.time.LocalDateTime;

public class AnalysisResult {
    public int id;
    public String projectPath;
    public String projectName;
    public String projectDescription;
    public String result;
    public LocalDateTime analyzedAt;

    public AnalysisResult(int id, String projectPath, String result, LocalDateTime analyzedAt) {
        this.id = id;
        this.projectPath = projectPath;
        this.result = result;
        this.analyzedAt = analyzedAt;
        
        // 从分析结果中提取项目名称和描述
        extractProjectInfoFromResult();
    }
    
    // 新增构造函数，支持直接设置项目名称和描述
    public AnalysisResult(int id, String projectPath, String projectName, String projectDescription, String result, LocalDateTime analyzedAt) {
        this.id = id;
        this.projectPath = projectPath;
        this.projectName = projectName;
        this.projectDescription = projectDescription;
        this.result = result;
        this.analyzedAt = analyzedAt;
    }
    
    /**
     * 从分析结果中提取项目名称和描述
     */
    public void extractProjectInfoFromResult() {
        if (result == null || result.isEmpty()) {
            this.projectName = "未知项目";
            this.projectDescription = "暂无描述";
            return;
        }
        
        // 处理可能包含Markdown代码块标记的响应
        String cleanResult = result.trim();
        if (cleanResult.startsWith("```json")) {
            cleanResult = cleanResult.substring(7);
        } else if (cleanResult.startsWith("```")) {
            cleanResult = cleanResult.substring(3);
        }
        
        if (cleanResult.endsWith("```")) {
            cleanResult = cleanResult.substring(0, cleanResult.length() - 3);
        }
        
        cleanResult = cleanResult.trim();
        
        try {
            // 尝试解析JSON结果并提取项目名称和摘要
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(cleanResult);
            
            // 提取项目名称
            if (root.has("project_name")) {
                this.projectName = root.get("project_name").asText("未知项目");
            } else {
                // 如果没有project_name字段，使用文件夹名作为默认值
                java.io.File folder = new java.io.File(projectPath);
                this.projectName = folder.getName();
            }
            
            // 提取项目描述（摘要）
            if (root.has("summary")) {
                this.projectDescription = root.get("summary").asText("暂无描述");
            } else {
                // 如果没有摘要字段，则使用结果的前100个字符作为描述
                String trimmed = cleanResult.length() > 100 ? cleanResult.substring(0, 100) + "..." : cleanResult;
                this.projectDescription = trimmed;
            }
        } catch (Exception e) {
            // 如果不是有效的JSON，则使用默认值
            java.io.File folder = new java.io.File(projectPath);
            this.projectName = folder.getName();
            String trimmed = cleanResult.length() > 100 ? cleanResult.substring(0, 100) + "..." : cleanResult;
            this.projectDescription = trimmed;
        }
    }
    
    @Override
    public String toString() {
        return "AnalysisResult{" +
                "id=" + id +
                ", projectPath='" + projectPath + '\'' +
                ", projectName='" + projectName + '\'' +
                ", projectDescription='" + projectDescription + '\'' +
                ", resultLength=" + (result != null ? result.length() : 0) +
                ", analyzedAt=" + analyzedAt +
                '}';
    }
}